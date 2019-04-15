package fr.free.nrw.commons.upload;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.net.Uri;
import android.support.annotation.IntDef;
import fr.free.nrw.commons.BasePresenter;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import java.lang.annotation.Retention;
import java.util.List;

public interface IUpload {

    public interface View {

        void setUploadItems(List<UploadModel.UploadItem> uploadItems);
        // Dummy implementation of the view interface to allow us to have a 'null object pattern'
        // in the presenter and avoid constant NULL checking.
//    UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
//    new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);

        @Retention(SOURCE)
        @IntDef({PLEASE_WAIT, TITLE_CARD, CATEGORIES, LICENSE})
        @interface UploadPage {

        }

        int PLEASE_WAIT = 0;

        int TITLE_CARD = 1;
        int CATEGORIES = 2;
        int LICENSE = 3;

        boolean checkIfLoggedIn();

        void setNextEnabled(boolean available);

        void setSubmitEnabled(boolean available);

        void setPreviousEnabled(boolean available);

        void setTopCardState(boolean state);

        void setRightCardVisibility(boolean visible);

        void setBottomCardState(boolean state);

        void setRightCardState(boolean bottomCardState);

        void setBackground(Uri mediaUri);

        void setTopCardVisibility(boolean visible);


        void updateRightCardContent(boolean gpsPresent);

        void updateBottomCardContent(int currentStep, int stepCount,
                UploadModel.UploadItem uploadItem, boolean isShowingItem);

        void updateLicenses(List<String> licenses, String selectedLicense);

        void updateLicenseSummary(String selectedLicense, int imageCount);

        void updateTopCardContent();

        void updateSubtitleVisibility(int imageCount);

        void dismissKeyboard();

        void showBadPicturePopup(String errorMessage);

        void showDuplicatePicturePopup();

        void finish();

        void launchMapActivity(String decCoords);

        void showErrorMessage(int resourceId);

        void initDefaultCategories();

        void showProgressDialog();

        void hideProgressDialog();

        void askUserToLogIn();
    }

    public interface UserActionListener extends BasePresenter<View> {

        void handleSubmit();


        UploadModel getUploadModel();

        List<String> getImageTitleList();

        BasicKvStore getDefaultKvStore();
    }
}
