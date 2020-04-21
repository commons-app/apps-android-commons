package fr.free.nrw.commons.upload.mediaDetails;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;
import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadModel.UploadItem;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract.UserActionListener;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract.View;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.lang.reflect.Proxy;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadMediaPresenter implements UserActionListener, SimilarImageInterface {

    private static final UploadMediaDetailsContract.View DUMMY = (UploadMediaDetailsContract.View) Proxy
            .newProxyInstance(
                    UploadMediaDetailsContract.View.class.getClassLoader(),
                    new Class[]{UploadMediaDetailsContract.View.class},
                    (proxy, method, methodArgs) -> null);

    private final UploadRepository repository;
    private UploadMediaDetailsContract.View view = DUMMY;

    private CompositeDisposable compositeDisposable;

    private Scheduler ioScheduler;
    private Scheduler mainThreadScheduler;

    @Inject
    public UploadMediaPresenter(UploadRepository uploadRepository,
                                @Named(IO_THREAD) Scheduler ioScheduler,
                                @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.repository = uploadRepository;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onAttachView(View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
        compositeDisposable.clear();
    }

    /**
     * Receives the corresponding uploadable file, processes it and return the view with and uplaod item
     *  @param uploadableFile
     * @param place
     */
    @Override
    public void receiveImage(UploadableFile uploadableFile, Place place) {
        view.showProgress(true);
        Disposable uploadItemDisposable = repository
                .preProcessImage(uploadableFile, place, this)
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(uploadItem ->
                        {
                            view.onImageProcessed(uploadItem, place);
                            ImageCoordinates gpsCoords = uploadItem.getGpsCoords();
                            view.showMapWithImageCoordinates(gpsCoords != null && gpsCoords.getImageCoordsExists());
                            view.showProgress(false);
                            if (gpsCoords != null && gpsCoords.getImageCoordsExists()) {
                                checkNearbyPlaces(uploadItem);
                            }
                        },
                        throwable -> Timber.e(throwable, "Error occurred in processing images"));
        compositeDisposable.add(uploadItemDisposable);
    }

    /**
     * This method checks for the nearest location that needs images and suggests it to the user.
     * @param uploadItem
     */
    private void checkNearbyPlaces(UploadItem uploadItem) {
        Disposable checkNearbyPlaces = Observable.fromCallable(() -> repository
                .checkNearbyPlaces(uploadItem.getGpsCoords().getDecLatitude(),
                        uploadItem.getGpsCoords().getDecLongitude()))
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(place -> view.onNearbyPlaceFound(uploadItem, place),
                        throwable -> Timber.e(throwable, "Error occurred in processing images"));
        compositeDisposable.add(checkNearbyPlaces);
    }

    /**
     * asks the repository to verify image quality
     *
     * @param uploadItem
     */
    @Override
    public void verifyImageQuality(UploadItem uploadItem) {
        view.showProgress(true);

        compositeDisposable.add(
            repository
                .getImageQuality(uploadItem)
                .observeOn(mainThreadScheduler)
                .subscribe(imageResult -> {
                            view.showProgress(false);
                            handleImageResult(imageResult);
                        },
                        throwable -> {
                            view.showProgress(false);
                            view.showMessage("" + throwable.getLocalizedMessage(),
                                    R.color.color_error);
                            Timber.e(throwable, "Error occurred while handling image");
                        })
        );
    }

    /**
     * Adds the corresponding upload item to the repository
     *
     * @param index
     * @param uploadItem
     */
    @Override
    public void setUploadItem(int index, UploadItem uploadItem) {
        repository.updateUploadItem(index, uploadItem);
    }

    /**
     * Fetches and sets the caption and desctiption of the previous item
     *
     * @param indexInViewFlipper
     */
    @Override
    public void fetchPreviousTitleAndDescription(int indexInViewFlipper) {
        UploadItem previousUploadItem = repository.getPreviousUploadItem(indexInViewFlipper);
        if (null != previousUploadItem) {
            view.setCaptionsAndDescriptions(previousUploadItem.getUploadMediaDetails());
        } else {
            view.showMessage(R.string.previous_image_title_description_not_found, R.color.color_error);
        }
    }

  @Override
  public void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex) {
    repository.useSimilarPictureCoordinates(imageCoordinates, uploadItemIndex);
  }

  /**
     * handles image quality verifications
     *
     * @param imageResult
     */
    public void handleImageResult(Integer imageResult) {
        if (imageResult == IMAGE_KEEP || imageResult == IMAGE_OK) {
            view.onImageValidationSuccess();
        } else {
            handleBadImage(imageResult);
        }
    }

    /**
     * Handle  images, say empty caption, duplicate file name, bad picture(in all other cases)
     *
     * @param errorCode
     */
    public void handleBadImage(Integer errorCode) {
        Timber.d("Handle bad picture with error code %d", errorCode);
        if (errorCode
                >= 8) { // If location of image and nearby does not match, then set shared preferences to disable wikidata edits
            repository.saveValue("Picture_Has_Correct_Location", false);
        }

        switch (errorCode) {
            case EMPTY_CAPTION:
                Timber.d("Captions are empty. Showing toast");
                view.showMessage(R.string.add_caption_toast, R.color.color_error);
                break;
            case FILE_NAME_EXISTS:
                Timber.d("Trying to show duplicate picture popup");
                view.showDuplicatePicturePopup();
                break;
            default:
                view.showBadImagePopup(errorCode);
        }
    }

    /**
     * notifies the user that a similar image exists
     * @param originalFilePath
     * @param possibleFilePath
     * @param similarImageCoordinates
     */
    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates) {
        view.showSimilarImageFragment(originalFilePath, possibleFilePath,
            similarImageCoordinates
        );
    }
}
