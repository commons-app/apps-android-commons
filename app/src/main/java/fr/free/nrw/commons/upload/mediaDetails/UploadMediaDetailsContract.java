package fr.free.nrw.commons.upload.mediaDetails;

import android.app.Activity;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadItem;
import java.util.List;

/**
 * The contract with with UploadMediaDetails and its presenter would talk to each other
 */
public interface UploadMediaDetailsContract {

    interface View extends SimilarImageInterface {

        void onImageProcessed(UploadItem uploadItem, Place place);

        void onNearbyPlaceFound(UploadItem uploadItem, Place place);

        void showProgress(boolean shouldShow);

        void onImageValidationSuccess();

        void showMessage(int stringResourceId, int colorResourceId);

        void showMessage(String message, int colorResourceId);

        void showDuplicatePicturePopup(UploadItem uploadItem);

        void showConnectionErrorPopup();

        void showExternalMap(UploadItem uploadItem);

        void showEditActivity(UploadItem uploadItem);

        void updateMediaDetails(List<UploadMediaDetail> uploadMediaDetails);

        void displayAddLocationDialog(Runnable runnable);
    }

    interface UserActionListener extends BasePresenter<View> {

        void receiveImage(UploadableFile uploadableFile, Place place, LatLng inAppPictureLocation);

        void setUploadMediaDetails(List<UploadMediaDetail> uploadMediaDetails, int uploadItemIndex);

        boolean verifyImageQuality(int uploadItemIndex, LatLng inAppPictureLocation);

        /**
         * Calculates the image quality
         *
         * @param uploadItemIndex Index of the UploadItem whose quality is to be checked
         * @param inAppPictureLocation In app picture location (if any)
         * @param activity Context reference
         * @return true if no internal error occurs, else returns false
         */
        boolean getImageQuality(int uploadItemIndex, LatLng inAppPictureLocation, Activity activity);

        void displayLocDialog(int uploadItemIndex, LatLng inAppPictureLocation);

        /**
         * Used to check image quality from stored qualities and display dialogs
         *
         * @param uploadItem UploadItem whose quality is to be checked
         * @param index Index of the UploadItem whose quality is to be checked
         */
        void checkImageQuality(UploadItem uploadItem, int index);

        /**
         * Updates the image qualities stored in JSON, whenever an image is deleted
         *
         * @param size Size of uploadableFiles
         * @param index Index of the UploadItem which was deleted
         */
        void updateImageQualitiesJSON(int size, int index);


        void copyTitleAndDescriptionToSubsequentMedia(int indexInViewFlipper);

        void fetchTitleAndDescription(int indexInViewFlipper);

        void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex);

        void onMapIconClicked(int indexInViewFlipper);

        void onEditButtonClicked(int indexInViewFlipper);

        void onUserConfirmedUploadIsOfPlace(Place place, int uploadItemPosition);
    }

}
