package fr.free.nrw.commons.upload;

import android.net.Uri;
import androidx.annotation.IntDef;
import fr.free.nrw.commons.location.LatLng;

import java.lang.annotation.Retention;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface UploadView {
    // Dummy implementation of the view interface to allow us to have a 'null object pattern'
    // in the presenter and avoid constant NULL checking.
//    UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
//    new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);

    List<Description> getDescriptions();

    @Retention(SOURCE)
    @IntDef({PLEASE_WAIT, TITLE_CARD, CATEGORIES, LICENSE})
    @interface UploadPage {}

    int PLEASE_WAIT = 0;

    int TITLE_CARD = 1;
    int CATEGORIES = 2;
    int LICENSE = 3;

    boolean checkIfLoggedIn();

    void updateThumbnails(List<UploadModel.UploadItem> uploads);

    void setNextEnabled(boolean available);

    void setSubmitEnabled(boolean available);

    void setPreviousEnabled(boolean available);

    void setTopCardState(boolean state);

    void setRightCardVisibility(boolean visible);

    void setBottomCardState(boolean state);

    void setBackground(Uri mediaUri);

    void setTopCardVisibility(boolean visible);

    void setBottomCardVisibility(boolean visible);

    void setBottomCardVisibility(@UploadPage int page, int uploadCount);

    void updateRightCardContent(boolean gpsPresent);

    void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem, boolean isShowingItem);

    void updateLicenses(List<String> licenses, String selectedLicense);

    void updateLicenseSummary(String selectedLicense, int imageCount);

    void updateTopCardContent();

    void updateSubtitleVisibility(int imageCount);

    void dismissKeyboard();

    void showBadPicturePopup(String errorMessage);

    void showDuplicatePicturePopup();

    void finish();

    void launchMapActivity(LatLng decCoords);

    void showErrorMessage(int resourceId);

    void initDefaultCategories();

    void showNoCategorySelectedWarning();

    void showProgressDialog();

    void hideProgressDialog();
}
