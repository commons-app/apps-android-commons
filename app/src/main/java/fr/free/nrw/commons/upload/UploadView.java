package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.reflect.Proxy;
import java.util.List;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public interface UploadView {
    // Dummy implementation of the view interface to allow us to have a 'null object pattern'
    // in the presenter and avoid constant NULL checking.
    UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
            new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);

    @Retention(SOURCE)
    @IntDef({PLEASE_WAIT, TITLE_CARD, CATEGORIES, LICENSE})
    @interface UploadPage {}

    int PLEASE_WAIT = 0;
    int TITLE_CARD = 1;
    int CATEGORIES = 2;
    int LICENSE = 3;

    void updateThumbnails(List<UploadModel.UploadItem> uploads);

    void setNextEnabled(boolean available);

    void setSubmitEnabled(boolean available);

    void setPreviousEnabled(boolean available);

    void setTopCardState(boolean state);

    void setBottomCardState(boolean state);

    void setBackground(Uri mediaUri);

    void setTopCardVisibility(boolean visible);

    void setBottomCardVisibility(boolean visible);

    void setBottomCardVisibility(@UploadPage int page);

    void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem);

    void updateLicenses(List<String> licenses, String selectedLicense);

    void updateLicenseSummary(String selectedLicense);

    void updateTopCardContent();

    void dismissKeyboard();
}
