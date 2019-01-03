package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.ImageUtils;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;
import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_TITLE;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter {

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private final MediaWikiApi mediaWikiApi;

    private static final UploadView DUMMY = (UploadView) Proxy.newProxyInstance(UploadView.class.getClassLoader(),
            new Class[]{UploadView.class}, (proxy, method, methodArgs) -> null);
    private UploadView view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE = (SimilarImageInterface) Proxy.newProxyInstance(SimilarImageInterface.class.getClassLoader(),
            new Class[]{SimilarImageInterface.class}, (proxy, method, methodArgs) -> null);
    private SimilarImageInterface similarImageInterface = SIMILAR_IMAGE;

    @UploadView.UploadPage
    private int currentPage = UploadView.PLEASE_WAIT;

    @Inject @Named("default_preferences")SharedPreferences prefs;

    @Inject
    UploadPresenter(UploadModel uploadModel,
                    UploadController uploadController,
                    MediaWikiApi mediaWikiApi) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
        this.mediaWikiApi = mediaWikiApi;
    }

    void receive(Uri mediaUri, String mimeType, String source) {
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
    void receive(List<Uri> media, String mimeType, @Contribution.FileSource String source) {
        Completable.fromRunnable(() -> uploadModel.receive(media, mimeType, source, similarImageInterface))
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
    void receiveDirect(Uri media, String mimeType, @Contribution.FileSource String source, String wikidataEntityIdPref, String title, String desc, String wikidataItemLocation) {
        Completable.fromRunnable(() -> uploadModel.receiveDirect(media, mimeType, source, wikidataEntityIdPref, title, desc, similarImageInterface, wikidataItemLocation))
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
    void selectLicense(String licenseName) {
        uploadModel.setSelectedLicense(licenseName);
        view.updateLicenseSummary(uploadModel.getSelectedLicense(), uploadModel.getCount());
    }

    //region Wizard step management

    /**
     * Called by the next button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    void handleNext(Title title,
                    List<Description> descriptions) {
        validateCurrentItemTitle()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(errorCode -> handleImage(errorCode, title, descriptions));
    }

    /**
     * Called by the next button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    void handleCategoryNext(CategoriesModel categoriesModel,
                    boolean noCategoryWarningShown) {
        if (categoriesModel.selectedCategoriesCount() < 1 && !noCategoryWarningShown) {
            view.showNoCategorySelectedWarning();
        } else {
            nextUploadedItem();
        }
    }

    private void handleImage(Integer errorCode, Title title, List<Description> descriptions) {
        switch (errorCode) {
            case EMPTY_TITLE:
                view.showErrorMessage(R.string.add_title_toast);
                break;
            case FILE_NAME_EXISTS:
                if(getCurrentItem().imageQuality.getValue().equals(IMAGE_KEEP)) {
                    setTitleAndDescription(title, descriptions);
                    nextUploadedItem();
                } else {
                    view.showDuplicatePicturePopup();
                }
                break;
            case IMAGE_OK:
            default:
                setTitleAndDescription(title, descriptions);
                nextUploadedItem();
        }
    }

    private void nextUploadedItem() {
        uploadModel.next();
        updateContent();
        if (uploadModel.isShowingItem()) {
            uploadModel.subscribeBadPicture(this::handleBadPicture);
        }
        view.dismissKeyboard();
    }

    private void setTitleAndDescription(Title title, List<Description> descriptions) {
        uploadModel.setCurrentTitleAndDescriptions(title, descriptions);
    }

    private Title getCurrentImageTitle() {
        return getCurrentItem().title;
    }

    String getCurrentImageFileName() {
        UploadItem currentItem = getCurrentItem();
        return currentItem.title + "." + uploadModel.getCurrentItem().fileExt;
    }

    @SuppressLint("CheckResult")
    private Observable<Integer> validateCurrentItemTitle() {
        Title title = getCurrentImageTitle();
        if (title.isEmpty()) {
            view.showErrorMessage(R.string.add_title_toast);
            return Observable.just(EMPTY_TITLE);
        }

        return Observable.fromCallable(() -> mediaWikiApi.fileExistsWithName(getCurrentImageFileName()))
                .subscribeOn(Schedulers.io())
                .map(doesFileExist -> {
                    if (doesFileExist) {
                        return FILE_NAME_EXISTS;
                    }
                    return IMAGE_OK;
                });
    }

    /**
     * Called by the previous button in {@link UploadActivity}
     */
    void handlePrevious() {
        uploadModel.previous();
        updateContent();
        if (uploadModel.isShowingItem()) {
            uploadModel.subscribeBadPicture(this::handleBadPicture);
        }
        view.dismissKeyboard();
    }

    /**
     * Called when one of the pictures on the top card is clicked on in {@link UploadActivity}
     */
    void thumbnailClicked(UploadItem item) {
        uploadModel.jumpTo(item);
        updateContent();
    }

    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    void handleSubmit(CategoriesModel categoriesModel) {
        if (view.checkIfLoggedIn())
            uploadModel.buildContributions(categoriesModel.getCategoryStringList())
                    .observeOn(Schedulers.io())
                    .subscribe(uploadController::startUpload);
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    void openCoordinateMap() {
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

    void keepPicture() {
        uploadModel.keepPicture();
    }

    void deletePicture() {
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
    void toggleTopCardState() {
        uploadModel.setTopCardState(!uploadModel.isTopCardState());
        view.setTopCardState(uploadModel.isTopCardState());
    }

    /**
     * Toggles the bottom card's state between open and closed.
     */
    void toggleBottomCardState() {
        uploadModel.setBottomCardState(!uploadModel.isBottomCardState());
        view.setBottomCardState(uploadModel.isBottomCardState());
    }

    /**
     * Toggles the right card's state between open and closed.
     */
    void toggleRightCardState() {
        uploadModel.setRightCardState(!uploadModel.isRightCardState());
        view.setRightCardState(uploadModel.isRightCardState());
    }

    /**
     * Sets all the cards' states to closed.
     */
    void closeAllCards() {
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
    public void init() {
        uploadController.prepareService();
    }

    void cleanup() {
        uploadController.cleanup();
    }

    void removeView() {
        this.view = DUMMY;
    }

    void addView(UploadView view) {
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
        String selectedLicense = prefs.getString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_3);
        view.updateLicenses(uploadModel.getLicenses(), selectedLicense);
        view.updateLicenseSummary(selectedLicense, uploadModel.getCount());
    }

    /**
     * Updates the cards and the background when a new currentPage is selected.
     */
    private void updateContent() {
        Timber.i("Updating content for currentPage" + uploadModel.getCurrentStep());
        view.setNextEnabled(uploadModel.isNextAvailable());
        view.setPreviousEnabled(uploadModel.isPreviousAvailable());
        view.setSubmitEnabled(uploadModel.isSubmitAvailable());

        view.setBackground(uploadModel.getCurrentItem().mediaUri);

        view.updateBottomCardContent(uploadModel.getCurrentStep(),
                uploadModel.getStepCount(),
                uploadModel.getCurrentItem(),
                uploadModel.isShowingItem());

        view.updateTopCardContent();

        GPSExtractor gpsObj = uploadModel.getCurrentItem().gpsCoords;
        view.updateRightCardContent(gpsObj != null && gpsObj.imageCoordsExists);

        view.updateSubtitleVisibility(uploadModel.getCount());

        showCorrectCards(uploadModel.getCurrentStep(), uploadModel.getCount());
    }

    /**
     * Updates the layout to show the correct bottom card.
     *
     * @param currentStep the current step
     * @param uploadCount how many items are being uploaded
     */
    private void showCorrectCards(int currentStep, int uploadCount) {
        if (uploadCount == 0) {
            currentPage = UploadView.PLEASE_WAIT;
        } else if (currentStep <= uploadCount) {
            currentPage = UploadView.TITLE_CARD;
            view.setTopCardVisibility(uploadModel.getCount() > 1);
        } else if (currentStep == uploadCount + 1) {
            currentPage = UploadView.CATEGORIES;
            view.setTopCardVisibility(false);
            view.setRightCardVisibility(false);
            view.initDefaultCategories();
        } else {
            currentPage = UploadView.LICENSE;
            view.setTopCardVisibility(false);
            view.setRightCardVisibility(false);
        }
        view.setBottomCardVisibility(currentPage, uploadCount);
    }

    //endregion

    /**
     * @return the item currently being displayed
     */
    private UploadItem getCurrentItem() {
        return uploadModel.getCurrentItem();
    }

    List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : uploadModel.getUploads()) {
            if (item.title.isSet()) {
                titleList.add(item.title.toString());
            }
        }
        return titleList;
    }

}