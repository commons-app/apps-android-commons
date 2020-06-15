package fr.free.nrw.commons.upload.mediaDetails;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.filepicker.UploadableFile;
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

        void onImageProcessed(UploadItem uploadItem);

        void onNearbyPlaceFound(UploadItem uploadItem, Place place);

        void showProgress(boolean shouldShow);

        void onImageValidationSuccess();

        void showMessage(int stringResourceId, int colorResourceId);

        void showMessage(String message, int colorResourceId);

        void showDuplicatePicturePopup(UploadItem uploadItem);

        void showBadImagePopup(Integer errorCode, UploadItem uploadItem);

        void showMapWithImageCoordinates(boolean shouldShow);

        void showExternalMap(UploadItem uploadItem);

        void updateMediaDetails(List<UploadMediaDetail> uploadMediaDetails);
    }

    interface UserActionListener extends BasePresenter<View> {

        void receiveImage(UploadableFile uploadableFile, Place place);

        void verifyImageQuality(int uploadItemIndex);

        void fetchPreviousTitleAndDescription(int indexInViewFlipper);

        void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex);

        void onMapIconClicked(int indexInViewFlipper);

        void onUserConfirmedUploadIsOfPlace(Place place, int uploadItemPosition);
    }

}
