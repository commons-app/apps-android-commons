package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

@Singleton
public class UploadPresenter {
    private static final String TOP_CARD_STATE = "fr.free.nrw.commons.upload.top_card_state";
    private static final String BOTTOM_CARD_STATE = "fr.free.nrw.commons.upload.bottom_card_state";

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private UploadView view;// = UploadView.DUMMY;


    @Inject
    public UploadPresenter(UploadModel uploadModel, UploadController uploadController) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
    }

    public void receive(Uri mediaUri, String mimeType, String source) {
        receive(Collections.singletonList(mediaUri), mimeType, source);
    }

    @SuppressLint("CheckResult")
    public void receive(List<Uri> media, String mimeType, String source) {
        Completable.fromRunnable(() -> uploadModel.receive(media, mimeType, source))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    updateCards(view);
                    updateLicenses(view);
                    updateContent();
                    if (uploadModel.isShowingItem())
                        uploadModel.subscribeBadPicture(this::handleBadPicture);
                });

    }

    public void imageTitleChanged(String text) {
        uploadModel.getCurrentItem().title = text;
        uploadModel.getCurrentItem().error = text.trim().isEmpty();
        view.updateTopCardContent();
    }

    public void selectLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
        view.updateLicenseSummary(uploadModel.getSelectedLicense());
    }

    public void descriptionChanged(String text) {
        uploadModel.getCurrentItem().description = text;
    }

    //region Wizard step management
    public void handleNext() {
        uploadModel.next();
        updateContent();
        if (uploadModel.isShowingItem()) uploadModel.subscribeBadPicture(this::handleBadPicture);
        view.dismissKeyboard();
    }

    public void handlePrevious() {
        uploadModel.previous();
        updateContent();
        if (uploadModel.isShowingItem()) uploadModel.subscribeBadPicture(this::handleBadPicture);
        view.dismissKeyboard();
    }

    public void thumbnailClicked(UploadModel.UploadItem item) {
        uploadModel.jumpTo(item);
        updateContent();
    }

    @SuppressLint("CheckResult")
    public void handleSubmit(CategoriesModel categoriesModel) {
        uploadModel.buildContributions(categoriesModel.getCategoryStringList())
                .observeOn(Schedulers.io())
                .subscribe(uploadController::startUpload);
    }

    public void openCoordinateMap() {
        GPSExtractor gpsObj = uploadModel.getCurrentItem().gpsCoords;
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(gpsObj.getDecLatitude() + "," + gpsObj.getDecLongitude());
        }
    }

    public void handleBadPicture(ImageUtils.Result result) {
        view.showBadPicturePopup(result);
    }

    public void keepPicture() {
        uploadModel.keepPicture();
    }

    public void deletePicture() {
        if (uploadModel.getCount() == 1)
            view.finish();
        else {
            uploadModel.deletePicture();
            updateCards(view);
            updateContent();
            if (uploadModel.isShowingItem())
                uploadModel.subscribeBadPicture(this::handleBadPicture);
            view.dismissKeyboard();
        }
    }
    //endregion

    //region Top Bottom and Right card state management
    public void toggleTopCardState() {
        uploadModel.setTopCardState(!uploadModel.isTopCardState());
        view.setTopCardState(uploadModel.isTopCardState());
    }

    public void toggleBottomCardState() {
        uploadModel.setBottomCardState(!uploadModel.isBottomCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    public void toggleRightCardState() {
        uploadModel.setRightCardState(!uploadModel.isRightCardState());
        view.setRightCardState(uploadModel.isRightCardState());
    }

    public void closeAllCards() {
        if (uploadModel.isTopCardState()) {
            uploadModel.setTopCardState(false);
            view.setTopCardState(false);
        }
        if (uploadModel.isRightCardState()) {
            uploadModel.setRightCardState(false);
            view.setRightCardState(false);
        }
        if (uploadModel.isBottomCardState()) {
            uploadModel.setBottomCardState(false);
            view.setBottomCardState(false);
        }
    }
    //endregion

    //region View / Lifecycle management
    public void initFromSavedState(Bundle state) {
        if (state != null) {
            Timber.i("Saved state is not null.");
            uploadModel.setTopCardState(state.getBoolean(TOP_CARD_STATE, true));
            uploadModel.setBottomCardState(state.getBoolean(BOTTOM_CARD_STATE, true));
        }
        uploadController.prepareService();
    }

    public void cleanup() {
        uploadController.cleanup();
    }

    public Bundle getSavedState() {
        Bundle bundle = new Bundle();
        bundle.putBoolean(TOP_CARD_STATE, uploadModel.isTopCardState());
        bundle.putBoolean(BOTTOM_CARD_STATE, uploadModel.isBottomCardState());
        return bundle;
    }

    public void removeView() {
        this.view = null;//UploadView.DUMMY;
    }

    public void addView(UploadView view) {
        this.view = view;

        updateCards(view);
        updateLicenses(view);
        updateContent();
    }

    void updateCards(UploadView view) {
        Timber.i("uploadModel.getCount():" + uploadModel.getCount());
        view.updateThumbnails(uploadModel.getUploads());
        view.setTopCardVisibility(uploadModel.getCount() > 1);
        view.setBottomCardVisibility(uploadModel.getCount() > 0);
        view.setTopCardState(uploadModel.isTopCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    void updateLicenses(UploadView view) {
        String selectedLicense = uploadModel.getSelectedLicense();
        view.updateLicenses(uploadModel.getLicenses(), selectedLicense);
        view.updateLicenseSummary(selectedLicense);
    }

    void updateContent() {
        Timber.i("Updating content for page" + uploadModel.getCurrentStep());
        view.setNextEnabled(uploadModel.isNextAvailable());
        view.setPreviousEnabled(uploadModel.isPreviousAvailable());
        view.setSubmitEnabled(uploadModel.isSubmitAvailable());

        view.setBackground(uploadModel.getCurrentItem().mediaUri);

        view.updateBottomCardContent(uploadModel.getCurrentStep(), uploadModel.getStepCount(), uploadModel.getCurrentItem());
        view.updateTopCardContent();

        showCorrectCards(uploadModel.getCurrentStep(), uploadModel.getCount());
    }

    void showCorrectCards(int currentStep, int uploadCount) {
        @UploadView.UploadPage int page;
        if (uploadCount == 0) {
            page = UploadView.PLEASE_WAIT;
        } else if (currentStep <= uploadCount) {
            page = UploadView.TITLE_CARD;
            view.setTopCardVisibility(uploadModel.getCount() > 1);
            view.setRightCardVisibility(true);
        } else if (currentStep == uploadCount + 1) {
            page = UploadView.CATEGORIES;
            view.setTopCardVisibility(false);
            view.setRightCardVisibility(false);
        } else {
            page = UploadView.LICENSE;
            view.setTopCardVisibility(false);
            view.setRightCardVisibility(false);
        }
        view.setBottomCardVisibility(page);
    }

    //endregion
    public UploadModel.UploadItem getCurrentItem() {
        return uploadModel.getCurrentItem();
    }
}