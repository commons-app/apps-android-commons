package fr.free.nrw.commons.upload;

import android.net.Uri;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class UploadPresenter {
    private static final String TOP_CARD_STATE = "fr.free.nrw.commons.upload.top_card_state";
    private static final String BOTTOM_CARD_STATE = "fr.free.nrw.commons.upload.bottom_card_state";

    private final UploadModel uploadModel;
    private UploadView view = UploadView.DUMMY;

    @Inject
    public UploadPresenter(UploadModel uploadModel) {
        this.uploadModel = uploadModel;
    }

    public void receive(Uri mediaUri, String mimeType, String source) {
        uploadModel.receive(Collections.singletonList(mediaUri), mimeType, source);
    }

    public void receive(List<Uri> mediaUri, String mimeType, String source) {
        uploadModel.receive(mediaUri, mimeType, source);
    }

    public void imageTitleChanged(String text) {
        uploadModel.getCurrentItem().title = text;
    }

    public void descriptionChanged(String text) {
        uploadModel.getCurrentItem().description = text;
    }

    //region Wizard step management
    public void handleNext() {
        uploadModel.next();
        updateContent();
        view.dismissKeyboard();
    }

    public void handlePrevious() {
        uploadModel.previous();
        updateContent();
        view.dismissKeyboard();
    }

    public void thumbnailClicked(UploadModel.UploadItem item) {
        uploadModel.jumpTo(item);
        updateContent();
    }
    //endregion

    //region Top and Bottom card state management
    public void toggleTopCardState() {
        uploadModel.setTopCardState(!uploadModel.isTopCardState());
        view.setTopCardState(uploadModel.isTopCardState());
    }

    public void toggleBottomCardState() {
        uploadModel.setBottomCardState(!uploadModel.isBottomCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    public void init(Bundle state) {
        if (state != null) {
            uploadModel.setTopCardState(state.getBoolean(TOP_CARD_STATE, true));
            uploadModel.setBottomCardState(state.getBoolean(BOTTOM_CARD_STATE, true));
        }
    }

    public Bundle getSavedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(TOP_CARD_STATE, uploadModel.isTopCardState());
        bundle.putBoolean(BOTTOM_CARD_STATE, uploadModel.isBottomCardState());
        return bundle;
    }
    //endregion

    //region View management
    public void removeView() {
        this.view = UploadView.DUMMY;
    }

    public void addView(UploadView view) {
        this.view = view;

        view.updateThumbnails(uploadModel.getUploads());
        view.setTopCardVisibility(uploadModel.getCount() > 1);

        view.setTopCardState(uploadModel.isTopCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());

        updateContent();
    }

    private void updateContent() {
        view.setNextEnabled(uploadModel.isNextAvailable());
        view.setPreviousEnabled(uploadModel.isPreviousAvailable());

        view.setBackground(uploadModel.getCurrentItem().mediaUri);

        view.updateBottomCardContent(uploadModel.getCurrentStep(), uploadModel.getStepCount(), uploadModel.getCurrentItem());
        view.setBottomCardVisibility(uploadModel.isShowingItem());
        view.updateTopCardContent();
    }
    //endregion
}