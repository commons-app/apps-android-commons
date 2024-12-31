package fr.free.nrw.commons.upload.mediaDetails

import android.app.Activity
import fr.free.nrw.commons.BasePresenter
import fr.free.nrw.commons.filepicker.UploadableFile
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.upload.ImageCoordinates
import fr.free.nrw.commons.upload.SimilarImageInterface
import fr.free.nrw.commons.upload.UploadItem
import fr.free.nrw.commons.upload.UploadMediaDetail

/**
 * The contract with with UploadMediaDetails and its presenter would talk to each other
 */
interface UploadMediaDetailsContract {
    interface View : SimilarImageInterface {
        fun onImageProcessed(uploadItem: UploadItem)

        fun onNearbyPlaceFound(uploadItem: UploadItem, place: Place?)

        fun showProgress(shouldShow: Boolean)

        fun onImageValidationSuccess()

        fun showMessage(stringResourceId: Int, colorResourceId: Int)

        fun showMessage(message: String?, colorResourceId: Int)

        fun showDuplicatePicturePopup(uploadItem: UploadItem)

        /**
         * Shows a dialog alerting the user that internet connection is required for upload process
         * Recalls UploadMediaPresenter.getImageQuality for all the next upload items,
         * if there is network connectivity and then the user presses okay
         */
        fun showConnectionErrorPopup()

        /**
         * Shows a dialog alerting the user that internet connection is required for upload process
         * Does nothing if there is network connectivity and then the user presses okay
         */
        fun showConnectionErrorPopupForCaptionCheck()

        fun showExternalMap(uploadItem: UploadItem)

        fun showEditActivity(uploadItem: UploadItem)

        fun updateMediaDetails(uploadMediaDetails: List<UploadMediaDetail>)

        fun displayAddLocationDialog(runnable: Runnable)
    }

    interface UserActionListener : BasePresenter<View?> {
        fun receiveImage(
            uploadableFile: UploadableFile?,
            place: Place?,
            inAppPictureLocation: LatLng?
        )

        fun setUploadMediaDetails(
            uploadMediaDetails: List<UploadMediaDetail>,
            uploadItemIndex: Int
        )

        /**
         * Calculates the image quality
         *
         * @param uploadItemIndex Index of the UploadItem whose quality is to be checked
         * @param inAppPictureLocation In app picture location (if any)
         * @param activity Context reference
         * @return true if no internal error occurs, else returns false
         */
        fun getImageQuality(
            uploadItemIndex: Int,
            inAppPictureLocation: LatLng?,
            activity: Activity
        ): Boolean

        /**
         * Checks if the image has a location. Displays a dialog alerting user that no location has
         * been to added to the image and asking them to add one, if location was not removed by the
         * user
         *
         * @param uploadItemIndex Index of the uploadItem which has no location
         * @param inAppPictureLocation In app picture location (if any)
         * @param hasUserRemovedLocation True if user has removed location from the image
         */
        fun displayLocDialog(
            uploadItemIndex: Int,
            inAppPictureLocation: LatLng?,
            hasUserRemovedLocation: Boolean
        )

        /**
         * Used to check image quality from stored qualities and display dialogs
         *
         * @param uploadItem UploadItem whose quality is to be checked
         * @param index Index of the UploadItem whose quality is to be checked
         */
        fun checkImageQuality(uploadItem: UploadItem, index: Int)

        /**
         * Updates the image qualities stored in JSON, whenever an image is deleted
         *
         * @param size Size of uploadableFiles
         * @param index Index of the UploadItem which was deleted
         */
        fun updateImageQualitiesJSON(size: Int, index: Int)

        fun copyTitleAndDescriptionToSubsequentMedia(indexInViewFlipper: Int)

        fun fetchTitleAndDescription(indexInViewFlipper: Int)

        fun useSimilarPictureCoordinates(imageCoordinates: ImageCoordinates, uploadItemIndex: Int)

        fun onMapIconClicked(indexInViewFlipper: Int)

        fun onEditButtonClicked(indexInViewFlipper: Int)

        fun onUserConfirmedUploadIsOfPlace(place: Place?, uploadItemIndex: Int)
    }
}
