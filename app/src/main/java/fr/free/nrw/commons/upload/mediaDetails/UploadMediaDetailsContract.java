package fr.free.nrw.commons.upload.mediaDetails;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
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

        void showDuplicatePicturePopup();

        void showBadImagePopup(Integer errorCode);

        void showMapWithImageCoordinates(boolean shouldShow);

        void setCaptionsAndDescriptions(List<UploadMediaDetail> uploadMediaDetails);
    }

    interface UserActionListener extends BasePresenter<View> {

        void receiveImage(UploadableFile uploadableFile, Place place);

        void verifyImageQuality(UploadItem uploadItem);

        void setUploadItem(int index, UploadItem uploadItem);

        void fetchPreviousTitleAndDescription(int indexInViewFlipper);

        void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex);

    }

}
