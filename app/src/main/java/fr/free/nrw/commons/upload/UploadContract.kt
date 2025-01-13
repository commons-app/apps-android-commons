package fr.free.nrw.commons.upload

import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.kvstore.BasicKvStore

/**
 * The contract using which the UplaodActivity would communicate with its presenter
 */
interface UploadContract {
    interface View {
        fun isLoggedIn(): Boolean

        fun finish()

        fun returnToMainActivity()

        /**
         * When submission successful, go to the loadProgressActivity to hint the user this
         * submission is valid. And the user will see the upload progress in this activity;
         * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5846)
         */
        fun goToUploadProgressActivity()

        fun askUserToLogIn()

        /**
         * Changes current image when one image upload is cancelled, to highlight next image in the top thumbnail.
         * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
         *
         * @param index Index of image to be removed
         * @param maxSize Max size of the `uploadableFiles`
         */
        fun highlightNextImageOnCancelledImage(index: Int, maxSize: Int)

        /**
         * Used to check if user has cancelled upload of any image in current upload
         * so that location compare doesn't show up again in same upload.
         * Fixes: [Issue](https://github.com/commons-app/apps-android-commons/issues/5511)
         *
         * @param isCancelled Is true when user has cancelled upload of any image in current upload
         */
        fun setImageCancelled(isCancelled: Boolean)

        fun showProgress(shouldShow: Boolean)

        fun showMessage(messageResourceId: Int)

        /**
         * Displays an alert with message given by `messageResourceId`.
         * `onPositiveClick` is run after acknowledgement.
         */
        fun showAlertDialog(messageResourceId: Int, onPositiveClick: Runnable)

        fun getUploadableFiles(): List<UploadableFile>?

        fun showHideTopCard(shouldShow: Boolean)

        fun onUploadMediaDeleted(index: Int)

        fun updateTopCardTitle()

        fun makeUploadRequest()
    }

    interface UserActionListener : BasePresenter<View> {
        fun handleSubmit()

        fun deletePictureAtIndex(index: Int)

        /**
         * Calls checkImageQuality of UploadMediaPresenter to check image quality of next image
         *
         * @param uploadItemIndex Index of next image, whose quality is to be checked
         */
        fun checkImageQuality(uploadItemIndex: Int)

        fun setupBasicKvStoreFactory(factory: (String) -> BasicKvStore)
    }
}
