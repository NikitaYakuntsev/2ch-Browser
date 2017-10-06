package ua.in.quireg.chan.asynctasks;

import android.app.Activity;
import android.os.AsyncTask;
import android.view.Window;

import ua.in.quireg.chan.R;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.exceptions.HtmlNotJsonException;
import ua.in.quireg.chan.interfaces.ICancelled;
import ua.in.quireg.chan.interfaces.IJsonApiReader;
import ua.in.quireg.chan.interfaces.IJsonProgressChangeListener;
import ua.in.quireg.chan.interfaces.IListView;
import ua.in.quireg.chan.models.domain.CaptchaEntity;
import ua.in.quireg.chan.models.domain.ThreadModel;
import ua.in.quireg.chan.services.RecaptchaService;

public class DownloadThreadsTask extends AsyncTask<Void, Long, Boolean> implements IJsonProgressChangeListener, ICancelled {

    static final String TAG = "DownloadThreadsTask";

    private final Activity mActivity;
    private final IJsonApiReader mJsonReader;
    private final IListView<ThreadModel[]> mView;
    private final String mBoard;
    private final int mPageNumberOrFilter;
    private final boolean mIsCheckModified;
    private final boolean mIsCatalog;

    private ThreadModel[] mThreadsList = null;
    private String mUserError = null;
    private CaptchaEntity mRecaptcha = null;
    // Progress bar
    private long mContentLength = 0;
    private long mProgressOffset = 0;
    private double mProgressScale = 1;

    public DownloadThreadsTask(Activity activity, IListView<ThreadModel[]> view, String board, boolean isCatalog, int pageNumberOrFilter, boolean checkModified, IJsonApiReader jsonReader) {
        this.mActivity = activity;
        this.mJsonReader = jsonReader;
        this.mView = view;
        this.mBoard = board;
        this.mIsCatalog = isCatalog;
        this.mPageNumberOrFilter = pageNumberOrFilter;
        this.mIsCheckModified = checkModified;
    }

    @Override
    protected Boolean doInBackground(Void... params) {
        // Читаем по ссылке json-объект со списком тредов
        try {
            if (this.mIsCatalog) {
                this.mThreadsList = this.mJsonReader.readCatalog(this.mBoard, this.mPageNumberOrFilter, this, this);
            } else {
                this.mThreadsList = this.mJsonReader.readThreadsList(this.mBoard, this.mPageNumberOrFilter, this.mIsCheckModified, this, this);
            }
            return true;
        } catch (HtmlNotJsonException he) {
            if (RecaptchaService.isCloudflareCaptchaPage(he.getHtml())) {
                this.mRecaptcha = RecaptchaService.loadCloudflareCaptcha();
            }
            if (this.mRecaptcha == null) {
                this.mUserError = he.getMessage();
            }
        } catch (Exception e) {
            this.mUserError = e.getMessage();

            if (StringUtils.areEqual(this.mUserError, "404 - Not Found") &&
                Constants.COOKIE_REQUIRE_BOARDS.contains(this.mBoard)) {
                this.mUserError = this.mActivity.getString(R.string.error_cookie_require_board);
            }
        }

        return false;
    }

    @Override
    public void onPreExecute() {
        // Отображаем экран загрузки и запускаем прогресс бар
        this.mView.showLoadingScreen();
    }

    @Override
    public void onPostExecute(Boolean success) {
        // Прячем все индикаторы загрузки
        this.mView.hideLoadingScreen();

        // Обновляем список или отображаем ошибку
        if (success) {
            this.mView.setData(this.mThreadsList);
        } else if (!success) {
            if (this.mRecaptcha != null) {
                this.mView.showCaptcha(this.mRecaptcha);
            } else {
                this.mView.showError(this.mUserError);
            }
        }
    }

    @Override
    public void onProgressUpdate(Long... progress) {
        // 0-9999 is ok, 10000 means it's finished
        if (this.mContentLength > 0) {
            double relativeProgress = progress[0].longValue() / (double) this.mContentLength;
            this.mView.setWindowProgress((int) (relativeProgress * 9999));
        }
    }

    @Override
    public void progressChanged(long newValue) {
        if (this.isCancelled()) {
            return;
        }

        long absoluteProgress = this.mProgressOffset + (long) (newValue * this.mProgressScale);
        this.publishProgress(absoluteProgress);
    }

    @Override
    public void indeterminateProgress() {
        this.mView.setWindowProgress(Window.PROGRESS_INDETERMINATE_ON);
    }

    @Override
    public void setContentLength(long value) {
        this.mContentLength = value;
    }

    @Override
    public long getContentLength() {
        return this.mContentLength;
    }

    @Override
    public void setOffsetAndScale(long offset, double scale) {
        this.mProgressOffset = offset;
        this.mProgressScale = scale;
    }
}
