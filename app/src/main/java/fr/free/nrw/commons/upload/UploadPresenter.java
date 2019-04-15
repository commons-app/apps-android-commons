package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;
import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_TITLE;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

import android.annotation.SuppressLint;
import android.content.Context;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.category.CategoriesModel;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.utils.StringUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements IUpload.UserActionListener {

    private static final IUpload.View DUMMY = (IUpload.View) Proxy.newProxyInstance(IUpload.View.class.getClassLoader(),
            new Class[]{IUpload.View.class}, (proxy, method, methodArgs) -> null);
    private IUpload.View view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE = (SimilarImageInterface) Proxy.newProxyInstance(SimilarImageInterface.class.getClassLoader(),
            new Class[]{SimilarImageInterface.class}, (proxy, method, methodArgs) -> null);
    private SimilarImageInterface similarImageInterface = SIMILAR_IMAGE;

    @IUpload.View.UploadPage
    private int currentPage = UploadView.PLEASE_WAIT;

    private final UploadModel uploadModel;
    private final UploadController uploadController;
    private final Context context;
    private final BasicKvStore defaultKvStore;
    private final JsonKvStore directKvStore;

    @Inject
    UploadPresenter(UploadModel uploadModel,
                    UploadController uploadController,
                    Context context,
                    @Named("default_preferences") BasicKvStore defaultKvStore,
                    @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore) {
        this.uploadModel = uploadModel;
        this.uploadController = uploadController;
        this.context = context;
        this.defaultKvStore = defaultKvStore;
        this.directKvStore = directKvStore;
    }

    String getCurrentImageFileName() {
        UploadItem currentItem = getCurrentItem();
        return currentItem.getFileName();
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
    @Override
    public void handleSubmit() {
        if (view.checkIfLoggedIn()) {
            uploadModel.buildContributions()
                    .observeOn(Schedulers.io())
                    .subscribe(uploadController::startUpload);
        }else{
            view.askUserToLogIn();
        }
    }

    /**
     * Called by the map button on the right card in {@link UploadActivity}
     */
    void openCoordinateMap() {
        GPSExtractor gpsObj = uploadModel.getCurrentItem().getGpsCoords();
        if (gpsObj != null && gpsObj.imageCoordsExists) {
            view.launchMapActivity(gpsObj.getDecLatitude() + "," + gpsObj.getDecLongitude());
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
           /* updateCards();
            updateContent();
            uploadModel.subscribeBadPicture(this::handleBadImage, false);
            view.dismissKeyboard();*/
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
//        view.setBottomCardVisibility(currentPage, uploadCount);
    }

    /**
     * @return the item currently being displayed
     */
    private UploadItem getCurrentItem() {
        return uploadModel.getCurrentItem();
    }

    public List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : uploadModel.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

    @Override public void onAttachView(IUpload.View view) {
        this.view=view;
        uploadController.prepareService();
    }

    @Override public void onDetachView() {
        this.view=DUMMY;
        uploadController.cleanup();
    }

    public BasicKvStore getDefaultKvStore() {
        return defaultKvStore;
    }

    public UploadModel getUploadModel() {
        return uploadModel;
    }

}