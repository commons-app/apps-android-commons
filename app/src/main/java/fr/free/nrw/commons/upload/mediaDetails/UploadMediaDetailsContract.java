package fr.free.nrw.commons.upload.mediaDetails;

import java.util.List;

import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.Description;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;

/**
 * The contract with with UploadMediaDetails and its presenter would talk to each other
 */
public interface UploadMediaDetailsContract {

    interface View extends SimilarImageInterface {

        void onImageProcessed(UploadItem uploadItem, Place place);

        void showProgress(boolean shouldShow);

        void onImageValidationSuccess();

        void showMessage(int stringResourceId, int colorResourceId);

        void showMessage(String message, int colorResourceId);

        void showDuplicatePicturePopup();

        void showBadImagePopup(Integer errorCode);

        void showMapWithImageCoordinates(boolean shouldShow);

        void setTitleAndDescription(String title, List<Description> descriptions);
    }

    interface UserActionListener extends BasePresenter<View> {

        void receiveImage(UploadableFile uploadableFile, @Contribution.FileSource String source,
                Place place);

        void verifyImageQuality(UploadItem uploadItem, boolean validateTitle);

        void setUploadItem(int index, UploadItem uploadItem);

        void fetchPreviousTitleAndDescription(int indexInViewFlipper);
    }

}
