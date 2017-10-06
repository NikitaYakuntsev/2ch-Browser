package ua.in.quireg.chan.services.presentation;

import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.presentation.DraftPostModel;

public class DraftPostsStorage {
    private String mBoardName = "";
    private String mThreadNumber = "";
    private DraftPostModel mDraft = null;

    public void saveDraft(String boardName, String threadNumber, DraftPostModel draft) {
        this.mBoardName = StringUtils.emptyIfNull(boardName);
        this.mThreadNumber = StringUtils.emptyIfNull(threadNumber);
        this.mDraft = draft;
    }

    public DraftPostModel getDraft(String boardName, String threadNumber) {
        if (this.mBoardName.equals(boardName) && this.mThreadNumber.equals(threadNumber)) {
            return this.mDraft;
        }

        return null;
    }

    public void clearDraft(String boardName, String threadNumber) {
        if (this.mBoardName.equals(boardName) && this.mThreadNumber.equals(threadNumber)) {
            this.mBoardName = "";
            this.mThreadNumber = "";
            this.mDraft = null;
        }
    }
}
