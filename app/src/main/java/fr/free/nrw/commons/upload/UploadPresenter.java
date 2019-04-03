package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import android.content.Context;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.CustomProxy;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;
import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_TITLE;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter {

    private static final UploadView DUMMY =
        (UploadView) CustomProxy.newInstance(UploadView.class.getClassLoader(),
            new Class[] { UploadView.class });

    private UploadView view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE =
        (SimilarImageInterface) CustomProxy.newInstance(
            SimilarImageInterface.class.getClassLoader(),
            new Class[] { SimilarImageInterface.class });
    private SimilarImageInterface similarImageInterface = SIMILAR_IMAGE;

    @UploadView.UploadPage
    private int currentPage = UploadView.PLEASE_WAIT;

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private final Context context;
    private final JsonKvStore directKvStore;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    UploadPresenter(UploadModel uploadModel,
                    UploadController uploadController,
                    Context context,
                    @Named("default_preferences") JsonKvStore directKvStore) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
        this.context = context;
        this.directKvStore = directKvStore;
    }

   /**
     * Passes the items received to {@link #uploadModel} and displays the items.
     *
     * @param media    The Uri's of the media being uploaded.
     * @param source   File source from {@link Contribution.FileSource}
     */
    @SuppressLint("CheckResult")
    void receive(List<UploadableFile> media,
                 @Contribution.FileSource String source,
                 Place place) {
        Observable<UploadItem> uploadItemObservable = uploadModel
                .preProcessImages(media, place, source, similarImageInterface);

        compositeDisposable.add(uploadItemObservable
                .toList()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(uploadItems -> onImagesProcessed(uploadItems, place),
                        throwable -> Timber.e(throwable, "Error occurred in processing images")));
    }

    private void onImagesProcessed(List<UploadItem> uploadItems, Place place) {
        uploadModel.onItemsProcessed(place, uploadItems);
        updateCards();
        updateLicenses();
        updateContent();
        uploadModel.subscribeBadPicture(this::handleBadImage, false);
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
        Timber.e("Inside handleNext");
        view.showProgressDialog();
        compositeDisposable.add(uploadModel.getImageQuality(uploadModel.getCurrentItem(), true)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(imageResult -> handleImage(title, descriptions, imageResult),
                        throwable -> Timber.e(throwable, "Error occurred while handling image")));
    }

    private void handleImage(Title title, List<Description> descriptions, Integer imageResult) {
        view.hideProgressDialog();
        if (imageResult == IMAGE_KEEP || imageResult == IMAGE_OK) {
            Timber.d("Set title and desc; Show next uploaded item");
            setTitleAndDescription(title, descriptions);
            directKvStore.putBoolean("Picture_Has_Correct_Location", true);
            nextUploadedItem();
        } else {
            handleBadImage(imageResult);
        }
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

    private void handleBadImage(Integer errorCode) {
        Timber.d("Handle bad picture with error code %d", errorCode);
        if (errorCode >= 8) { // If location of image and nearby does not match, then set shared preferences to disable wikidata edits
            directKvStore.putBoolean("Picture_Has_Correct_Location", false);
        }

        switch (errorCode) {
            case EMPTY_TITLE:
                Timber.d("Title is empty. Showing toast");
                view.showErrorMessage(R.string.add_title_toast);
                break;
            case FILE_NAME_EXISTS:
                Timber.d("Trying to show duplicate picture popup");
                view.showDuplicatePicturePopup();
                break;
            default:
                String errorMessageForResult = getErrorMessageForResult(context, errorCode);
                if (StringUtils.isBlank(errorMessageForResult)) {
                    return;
                }
                view.showBadPicturePopup(errorMessageForResult);
        }
    }

    private void nextUploadedItem() {
        Timber.d("Trying to show next uploaded item");
        uploadModel.next();
        updateContent();
        uploadModel.subscribeBadPicture(this::handleBadImage, false);
        view.dismissKeyboard();
    }

    private void setTitleAndDescription(Title title, List<Description> descriptions) {
        Timber.d("setTitleAndDescription: Setting title and desc");
        uploadModel.setCurrentTitleAndDescriptions(title, descriptions);
    }

    String getCurrentImageFileName() {
        UploadItem currentItem = getCurrentItem();
        return currentItem.getFileName();
    }

    /**
     * Called by the previous button in {@link UploadActivity}
     */
    void handlePrevious() {
        uploadModel.previous();
        updateContent();
        uploadModel.subscribeBadPicture(this::handleBadImage, false);
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
            compositeDisposable.add(uploadModel.buildContributions(categoriesModel.getCategoryStringList())
                    .observeOn(Schedulers.io())
                    .subscribe(uploadController::startUpload));
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    void openCoordinateMap() {
        GPSExtractor gpsObj = uploadModel.getCurrentItem().getGpsCoords();
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(new LatLng(gpsObj.getDecLatitude(), gpsObj.getDecLongitude(), 0.0f));
        }
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
            uploadModel.subscribeBadPicture(this::handleBadImage, false);
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
     * Sets all the cards' states to closed.
     */
    void closeAllCards() {
        if (uploadModel.isTopCardState()) {
            uploadModel.setTopCardState(false);
            view.setTopCardState(false);
        }
        if (uploadModel.isRightCardState()) {
            uploadModel.setRightCardState(false);
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
        compositeDisposable.clear();
        uploadModel.cleanup();
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
        String selectedLicense = directKvStore.getString(Prefs.DEFAULT_LICENSE,
            Prefs.Licenses.CC_BY_SA_4);//CC_BY_SA_4 is the default one used by the commons web app
        try {//I have to make sure that the stored default license was not one of the deprecated one's
            Utils.licenseNameFor(selectedLicense);
        } catch (IllegalStateException exception) {
            Timber.e(exception.getMessage());
            selectedLicense = Prefs.Licenses.CC_BY_SA_4;
            directKvStore.putString(Prefs.DEFAULT_LICENSE, Prefs.Licenses.CC_BY_SA_4);
        }
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

        view.setBackground(uploadModel.getCurrentItem().getMediaUri());

        view.updateBottomCardContent(uploadModel.getCurrentStep(),
                uploadModel.getStepCount(),
                uploadModel.getCurrentItem(),
                uploadModel.isShowingItem());

        view.updateTopCardContent();

        GPSExtractor gpsObj = uploadModel.getCurrentItem().getGpsCoords();
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
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

}