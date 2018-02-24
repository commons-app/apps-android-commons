package fr.free.nrw.commons.upload;

import android.net.Uri;

import java.lang.reflect.Proxy;
import java.util.List;

public interface UploadView {
    UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
            new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);

    void updateThumbnails(List<UploadModel.UploadItem> uploads);

    void setNextEnabled(boolean available);

    void setPreviousEnabled(boolean available);

    void setTopCardState(boolean state);

    void setBottomCardState(boolean state);

    void setBackground(Uri mediaUri);

    void setTopCardVisibility(boolean visible);

    void setBottomCardVisibility(boolean visible);

    void updateBottomCardContent(int currentStep, int stepCount, UploadModel.UploadItem uploadItem);

    void updateTopCardContent();

    void dismissKeyboard();
}
