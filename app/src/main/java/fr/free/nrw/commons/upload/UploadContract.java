package fr.free.nrw.commons.upload;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.filepicker.UploadableFile;

/**
 * The contract using which the UplaodActivity would communicate with its presenter
 */
public interface UploadContract {

    public interface View {

        boolean isLoggedIn();

        void finish();

        void returnToMainActivity();

        void askUserToLogIn();

        /**
         * Changes current image when one image upload is cancelled, to highlight next image in the top thumbnail.
         * Fixes: <a href="https://github.com/commons-app/apps-android-commons/issues/5511">Issue</a>
         *
         * @param index Index of image to be removed
         * @param maxSize Max size of the {@code uploadableFiles}
         */
        void highlightNextImageOnCancelledImage(int index, int maxSize);

        /**
         * Used to check if user has cancelled upload of any image in current upload
         * so that location compare doesn't show up again in same upload.
         * Fixes: <a href="https://github.com/commons-app/apps-android-commons/issues/5511">Issue</a>
         *
         * @param isCancelled Is true when user has cancelled upload of any image in current upload
         */
        void setImageCancelled(boolean isCancelled);

        void showProgress(boolean shouldShow);

        void showMessage(int messageResourceId);

        /**
         * Displays an alert with message given by {@code messageResourceId}.
         * {@code onPositiveClick} is run after acknowledgement.
         */
        void showAlertDialog(int messageResourceId, Runnable onPositiveClick);

        List<UploadableFile> getUploadableFiles();

        void showHideTopCard(boolean shouldShow);

        void onUploadMediaDeleted(int index);

        void updateTopCardTitle();

        void makeUploadRequest();
    }

    public interface UserActionListener extends BasePresenter<View> {

        void handleSubmit();

        void deletePictureAtIndex(int index);
    }
}
