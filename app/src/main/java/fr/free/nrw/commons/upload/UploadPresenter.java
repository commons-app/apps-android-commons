package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.net.Uri;
import android.os.Bundle;

import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Completable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter {
    private static final String TOP_CARD_STATE = "fr.free.nrw.commons.upload.top_card_state";
    private static final String BOTTOM_CARD_STATE = "fr.free.nrw.commons.upload.bottom_card_state";

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private static final UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
            new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);
    private UploadView view = DUMMY;


    @Inject
    public UploadPresenter(UploadModel uploadModel, UploadController uploadController) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
    }

    public void receive(Uri mediaUri, String mimeType, String source) {
        receive(Collections.singletonList(mediaUri), mimeType, source);
    }

    /**
     * Passes the items received to {@link #uploadModel} and displays the items.
     *
     * @param media    The Uri's of the media being uploaded.
     * @param mimeType the mimeType of the files.
     * @param source   File source from {@link Contribution.FileSource}
     */
    @SuppressLint("CheckResult")
    public void receive(List<Uri> media, String mimeType, @Contribution.FileSource String source) {
        Completable.fromRunnable(() -> uploadModel.receive(media, mimeType, source))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    updateCards();
                    updateLicenses();
                    updateContent();
                    if (uploadModel.isShowingItem())
                        uploadModel.subscribeBadPicture(this::handleBadPicture);
                }, Timber::e);
    }

    /**
     * Passes the direct upload item received to {@link #uploadModel} and displays the items.
     *
     * @param media The Uri's of the media being uploaded.
     * @param mimeType the mimeType of the files.
     * @param source File source from {@link Contribution.FileSource}
     */
    @SuppressLint("CheckResult")
    public void receiveDirect(Uri media, String mimeType, @Contribution.FileSource String source, String wikidataEntityIdPref, String title, String desc) {
        Completable.fromRunnable(() -> uploadModel.receiveDirect(media, mimeType, source, wikidataEntityIdPref, title, desc))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(() -> {
                    updateCards();
                    updateLicenses();
                    updateContent();
                    if (uploadModel.isShowingItem())
                        uploadModel.subscribeBadPicture(this::handleBadPicture);
                }, Timber::e);
    }
    /**
     * Sets the license to parameter and updates {@link UploadActivity}
     *
     * @param licenseName license name
     */
    public void selectLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
        view.updateLicenseSummary(uploadModel.getSelectedLicense());
    }

    //region Wizard step management

    /**
     * Called by the next button in {@link UploadActivity}
     */
    public void handleNext() {
        uploadModel.next();
        updateContent();
        if (uploadModel.isShowingItem()) uploadModel.subscribeBadPicture(this::handleBadPicture);
        view.dismissKeyboard();
    }

    /**
     * Called by the previous button in {@link UploadActivity}
     */
    public void handlePrevious() {
        uploadModel.previous();
        updateContent();
        if (uploadModel.isShowingItem()) uploadModel.subscribeBadPicture(this::handleBadPicture);
        view.dismissKeyboard();
    }

    /**
     * Called when one of the pictures on the top card is clicked on in {@link UploadActivity}
     */
    public void thumbnailClicked(UploadModel.UploadItem item) {
        uploadModel.jumpTo(item);
        updateContent();
    }

    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    public void handleSubmit(CategoriesModel categoriesModel) {
        if (view.checkIfLoggedIn())
            uploadModel.buildContributions(categoriesModel.getCategoryStringList())
                    .observeOn(Schedulers.io())
                    .subscribe(uploadController::startUpload);
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    public void openCoordinateMap() {
        GPSExtractor gpsObj = uploadModel.getCurrentItem().gpsCoords;
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(gpsObj.getDecLatitude() + "," + gpsObj.getDecLongitude());
        }
    }


    /**
     * Called by the image processors when a result is obtained.
     *
     * @param result the result returned by the image procesors.
     */
    private void handleBadPicture(@ImageUtils.Result int result) {
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
            updateCards();
            updateContent();
            if (uploadModel.isShowingItem())
                uploadModel.subscribeBadPicture(this::handleBadPicture);
            view.dismissKeyboard();
        }
    }
    //endregion

    //region Top Bottom and Right card state management


    /**
     * Toggles the top card's state between open and closed.
     */
    public void toggleTopCardState() {
        uploadModel.setTopCardState(!uploadModel.isTopCardState());
        view.setTopCardState(uploadModel.isTopCardState());
    }

    /**
     * Toggles the bottom card's state between open and closed.
     */
    public void toggleBottomCardState() {
        uploadModel.setBottomCardState(!uploadModel.isBottomCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    /**
     * Toggles the right card's state between open and closed.
     */
    public void toggleRightCardState() {
        uploadModel.setRightCardState(!uploadModel.isRightCardState());
        view.setRightCardState(uploadModel.isRightCardState());
    }

    /**
     * Sets all the cards' states to closed.
     */
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
        this.view = DUMMY;
    }

    public void addView(UploadView view) {
        this.view = view;

        updateCards();
        updateLicenses();
        updateContent();
    }


    /**
     * Updates the cards for when there is a change to the amount of items being uploaded.
     */
    private void updateCards() {
        Timber.i("uploadModel.getCount():" + uploadModel.getCount());
        view.updateThumbnails(uploadModel.getUploads());
        view.setTopCardVisibility(uploadModel.getCount() > 1);
        view.setBottomCardVisibility(uploadModel.getCount() > 0);
        view.setTopCardState(uploadModel.isTopCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    /**
     * Sets the list of licences and the default license.
     */
    private void updateLicenses() {
        String selectedLicense = uploadModel.getSelectedLicense();
        view.updateLicenses(uploadModel.getLicenses(), selectedLicense);
        view.updateLicenseSummary(selectedLicense);
    }

    /**
     * Updates the cards and the background when a new page is selected.
     */
    private void updateContent() {
        Timber.i("Updating content for page" + uploadModel.getCurrentStep());
        view.setNextEnabled(uploadModel.isNextAvailable());
        view.setPreviousEnabled(uploadModel.isPreviousAvailable());
        view.setSubmitEnabled(uploadModel.isSubmitAvailable());

        view.setBackground(uploadModel.getCurrentItem().mediaUri);

        view.updateBottomCardContent(uploadModel.getCurrentStep(), uploadModel.getStepCount(), uploadModel.getCurrentItem());
        view.updateTopCardContent();

        showCorrectCards(uploadModel.getCurrentStep(), uploadModel.getCount());
    }

    /**
     * Updates the layout to show the correct bottom card.
     *
     * @param currentStep the current step
     * @param uploadCount how many items are being uploaded
     */
    private void showCorrectCards(int currentStep, int uploadCount) {
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

    /**
     * @return the item currently being displayed
     */
    public UploadModel.UploadItem getCurrentItem() {
        return uploadModel.getCurrentItem();
    }

}