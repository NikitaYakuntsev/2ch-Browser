package ua.in.quireg.chan.models.presentation;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.Resources.Theme;
import android.text.SpannableStringBuilder;
import android.text.style.URLSpan;

import ua.in.quireg.chan.R;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.common.library.MyHtml;
import ua.in.quireg.chan.common.library.MyLog;
import ua.in.quireg.chan.common.utils.HtmlUtils;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.common.utils.ThreadPostUtils;
import ua.in.quireg.chan.interfaces.IURLSpanClickListener;
import ua.in.quireg.chan.interfaces.IUrlBuilder;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.BadgeModel;
import ua.in.quireg.chan.models.domain.PostModel;
import ua.in.quireg.chan.services.presentation.FlowTextHelper;
import ua.in.quireg.chan.settings.ApplicationSettings;

public class PostItemViewModel implements IPostListEntity {
    private static final Pattern sReplyLinkFullPattern = Pattern.compile("<a.*?href=\".+?#(\\d+)\".*?>(?:>>|&gt;&gt;).+?</a>");

    private final IWebsite mWebsite;
    private final String mBoardName;
    private final String mThreadNumber;
    private final int mPosition;
    private final PostModel mModel;
    private final Theme mTheme;
    private final IURLSpanClickListener mUrlListener;
    private final Resources mResources = Factory.resolve(Resources.class);
    private final ApplicationSettings mSettings = Factory.resolve(ApplicationSettings.class);
    private final IUrlBuilder mUrlBuilder;

    private final SpannableStringBuilder mSpannedComment;
    private SpannableStringBuilder mCachedReferencesString = null;
    private AttachmentInfo[] mAttachments = new AttachmentInfo[Constants.MAX_ATTACHMENTS];
    private String mPostDate = null;
    private final BadgeModel mBadge;

    private final ArrayList<String> refersTo = new ArrayList<String>();
    private final ArrayList<String> referencesFrom = new ArrayList<String>();

    public boolean isFloatImageComment = false;
    private boolean mIsLocalDateTime = false;
    private boolean mHasUrlSpans = false;
    private boolean mIsLongTextExpanded = false;

    public PostItemViewModel(IWebsite website, String boardName, String threadNumber, int position, PostModel model, Theme theme, IURLSpanClickListener listener) {
        this.mModel = model;
        this.mTheme = theme;
        this.mUrlListener = listener;
        this.mPosition = position;
        this.mWebsite = website;
        this.mBoardName = boardName;
        this.mThreadNumber = threadNumber;
        this.mUrlBuilder = this.mWebsite.getUrlBuilder();

        this.parseReferences();
        this.mBadge = this.mModel.getBadge();
        this.mSpannedComment = this.createSpannedComment();
    }

    public String getSubjectOrText() {
        String subject = this.mModel.getSubject();
        if (!StringUtils.isEmpty(subject)) {
            return subject;
        }

        return StringUtils.cutIfLonger(StringUtils.emptyIfNull(this.getSpannedComment()), 50);
    }

    private void parseReferences() {
        String comment = this.mModel.getComment();

        if (comment == null) {
            MyLog.v("PostItemViewModel", "comment == null");
            return;
        }

        Matcher m = sReplyLinkFullPattern.matcher(comment);
        while (m.find()) {
            if (m.groupCount() > 0 && !this.refersTo.contains(m.group(1))) {
                this.refersTo.add(m.group(1));
            }
        }
    }

    private SpannableStringBuilder createSpannedComment() {
        if (StringUtils.isEmpty(this.mModel.getComment())) {
            return new SpannableStringBuilder("");
        }

        String fixedComment = HtmlUtils.fixHtmlTags(this.mModel.getComment());
        SpannableStringBuilder builder = HtmlUtils.createSpannedFromHtml(fixedComment, this.mTheme, this.mUrlBuilder);

        URLSpan[] urlSpans = builder.getSpans(0, builder.length(), URLSpan.class);
        if (urlSpans.length > 0) {
            this.mHasUrlSpans = true;
            HtmlUtils.replaceUrls(builder, this.mUrlListener, this.mTheme);
        }

        return builder;
    }

    public void makeCommentFloat(FloatImageModel floatModel) {
        // Игнорируем, если был уже сделан или у поста нет прикрепленного файла
        if (this.canMakeCommentFloat()) {
            this.isFloatImageComment = true;
            FlowTextHelper.tryFlowText(this.getSpannedComment(), floatModel);
        }
    }

    public void addReferenceFrom(String postNumber) {
        this.referencesFrom.add(postNumber);
        this.mCachedReferencesString = null;
    }

    public int getPosition() {
        return this.mPosition;
    }

    public IWebsite getWebsite() {
        return this.mWebsite;
    }

    public String getBoardName() {
        return this.mBoardName;
    }

    public String getThreadNumber() {
        return this.mThreadNumber;
    }

    public String getNumber() {
        return this.mModel.getNumber();
    }

    public BadgeModel getBadge() {
        return this.mBadge;
    }

    public String getParentThreadNumber() {
        String parent = this.mModel.getParentThread();
        return parent != null && !parent.equals("0") ? parent : this.getNumber();
    }

    public String getName() {
        return this.mModel.getName();
    }

    public String getTrip() {
        return this.mModel.getTrip();
    }

    public String getSubject() {
        return StringUtils.emptyIfNull(this.mModel.getSubject());
    }

    public boolean isSage() {
        return this.mModel.getSage();
    }

    public boolean isOp() {
        return this.mModel.isOp();
    }

    public boolean hasAttachment() {
        return ThreadPostUtils.hasAttachment(this.mModel);
    }

    public int getAttachmentsNumber() {
        return this.mModel.getAttachments().size();
    }

    public AttachmentInfo getAttachment(int index) {
        if(index >= this.getAttachmentsNumber()) {
            return null;
        }

        if (this.mAttachments[index] == null) {
            this.mAttachments[index] = new AttachmentInfo(this.mModel.getAttachments().get(index), this.mWebsite, this.mBoardName, this.mThreadNumber);
        }

        return this.mAttachments[index];
    }

    public SpannableStringBuilder getSpannedComment() {
        return this.mSpannedComment;
    }

    public ArrayList<String> getRefersTo() {
        return this.refersTo;
    }

    public String getPostDate(Context context) {
        if (this.mPostDate == null || this.mIsLocalDateTime != this.mSettings.isLocalDateTime()) {
            this.mIsLocalDateTime = this.mSettings.isLocalDateTime();
            String formattedDate = this.mIsLocalDateTime
                    ? ThreadPostUtils.getLocalDateFromTimestamp(context, this.mModel.getTimestamp())
                    : ThreadPostUtils.getMoscowDateFromTimestamp(context, this.mModel.getTimestamp());

            this.mPostDate = formattedDate;
        }

        return this.mPostDate;
    }

    public boolean hasUrls() {
        return this.mHasUrlSpans;
    }

    public boolean hasReferencesFrom() {
        return !this.referencesFrom.isEmpty();
    }

    public boolean isLongTextExpanded() {
        return this.mIsLongTextExpanded;
    }

    public void setLongTextExpanded(boolean isExpanded) {
        this.mIsLongTextExpanded = isExpanded;
    }

    public SpannableStringBuilder getReferencesFromAsSpannableString() {
        if (this.mCachedReferencesString == null) {
            String firstWord = this.mResources.getString(R.string.postitem_replies);
            this.mCachedReferencesString = this.createReferencesString(firstWord, this.referencesFrom);
        }

        return this.mCachedReferencesString;
    }

    private SpannableStringBuilder createReferencesString(String firstWord, ArrayList<String> references) {
        if (references.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(firstWord);
        sb.append(" ");

        Iterator<String> iterator = references.iterator();
        // Собираю список ссылок в одну строку, разделенную запятыми
        while (iterator.hasNext()) {
            String refNumber = iterator.next();

            String refUrl = this.mUrlBuilder.getPostUrlHtml(this.mBoardName, this.mThreadNumber, refNumber);
            sb.append("<a href=\"" + refUrl + "\">" + "&gt;&gt;" + refNumber + "</a>");

            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        // Разбираю строку на объекты-ссылки и добавляю обработчики событий
        String joinedLinks = sb.toString();
        SpannableStringBuilder builder = (SpannableStringBuilder) MyHtml.fromHtml(joinedLinks);
        HtmlUtils.replaceUrls(builder, this.mUrlListener, this.mTheme);

        return builder;
    }

    /**
     * Можно поставить обтекание текста если версия 2.2 и к посту прикреплено
     * изображение
     */
    public boolean canMakeCommentFloat() {
        return FlowTextHelper.sNewClassAvailable && !this.isFloatImageComment && (this.getAttachmentsNumber() == 1);
    }

    public boolean isCommentFloat() {
        return this.isFloatImageComment;
    }

    public boolean isListItemEnabled() {
        return true;
    }
}
