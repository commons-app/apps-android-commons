package fr.free.nrw.commons.upload.mediaDetails;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;
import static fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.activity;
import static fr.free.nrw.commons.utils.ImageUtils.EMPTY_CAPTION;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_KEEP;
import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

import android.app.Activity;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageInterface;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadItem;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailFragment.UploadMediaDetailFragmentCallback;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract.UserActionListener;
import fr.free.nrw.commons.upload.mediaDetails.UploadMediaDetailsContract.View;
import fr.free.nrw.commons.utils.DialogUtil;
import io.github.coordinates2country.Coordinates2Country;
import io.reactivex.Maybe;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.lang.reflect.Proxy;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;
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

    private final JsonKvStore defaultKVStore;
    private Scheduler ioScheduler;
    private Scheduler mainThreadScheduler;

    public static UploadMediaDetailFragmentCallback presenterCallback ;

    private final List<String> WLM_SUPPORTED_COUNTRIES= Arrays.asList("am","at","az","br","hr","sv","fi","fr","de","gh","in","ie","il","mk","my","mt","pk","pe","pl","ru","rw","si","es","se","tw","ug","ua","us");
    private Map<String, String> countryNamesAndCodes = null;

    private final String keyForCurrentUploadImageQualities = "UploadedImagesQualities";

    /**
     * Variable used to determine if the battery-optimisation dialog is being shown or not
     */
    public static boolean isBatteryDialogShowing;

    public static boolean isCategoriesDialogShowing;

    @Inject
    public UploadMediaPresenter(UploadRepository uploadRepository,
        @Named("default_preferences") JsonKvStore defaultKVStore,
                                @Named(IO_THREAD) Scheduler ioScheduler,
                                @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.repository = uploadRepository;
        this.defaultKVStore = defaultKVStore;
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
     * Sets the Upload Media Details for the corresponding upload item
     *
     * @param uploadMediaDetails
     * @param uploadItemIndex
     */
    @Override
    public void setUploadMediaDetails(List<UploadMediaDetail> uploadMediaDetails, int uploadItemIndex) {
        repository.getUploads().get(uploadItemIndex).setMediaDetails(uploadMediaDetails);
    }

    /**
     * Receives the corresponding uploadable file, processes it and return the view with and uplaod item
     *  @param uploadableFile
     * @param place
     */
    @Override
    public void receiveImage(final UploadableFile uploadableFile, final Place place,
                            LatLng inAppPictureLocation) {
        view.showProgress(true);
        compositeDisposable.add(
            repository
                .preProcessImage(uploadableFile, place, this, inAppPictureLocation)
                .map(uploadItem -> {
                    if(place!=null && place.isMonument()){
                        if (place.location != null) {
                            final String countryCode = reverseGeoCode(place.location);
                            if (countryCode != null && WLM_SUPPORTED_COUNTRIES
                                .contains(countryCode.toLowerCase())) {
                                uploadItem.setWLMUpload(true);
                                uploadItem.setCountryCode(countryCode.toLowerCase());
                            }
                        }
                    }
                    return uploadItem;
                })
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(uploadItem ->
                    {
                        view.onImageProcessed(uploadItem, place);
                        view.updateMediaDetails(uploadItem.getUploadMediaDetails());
                        view.showProgress(false);
                        final ImageCoordinates gpsCoords = uploadItem.getGpsCoords();
                        final boolean hasImageCoordinates =
                            gpsCoords != null && gpsCoords.getImageCoordsExists();
                        if (hasImageCoordinates && place == null) {
                            checkNearbyPlaces(uploadItem);
                        }
                    },
                    throwable -> Timber.e(throwable, "Error occurred in processing images")));
    }

    @Nullable
    private String reverseGeoCode(final LatLng latLng){
        if(countryNamesAndCodes == null){
            countryNamesAndCodes = getCountryNamesAndCodes();
        }
        return countryNamesAndCodes.get(Coordinates2Country.country(latLng.getLatitude(), latLng.getLongitude()));
    }

    /**
     * Creates HashMap containing all ISO countries 2-letter codes provided by <code>Locale.getISOCountries()</code>
     * and their english names
     *
     * @return HashMap where Key is country english name and Value is 2-letter country code
     * e.g. ["Germany":"DE", ...]
     */
    private Map<String, String> getCountryNamesAndCodes(){
        final Map<String, String> result = new HashMap<>();

        final String[] isoCountries = Locale.getISOCountries();

        for (final String isoCountry : isoCountries) {
            result.put(
                new Locale("en", isoCountry).getDisplayCountry(Locale.ENGLISH),
                isoCountry
            );
        }

        return result;
    }

    /**
     * This method checks for the nearest location that needs images and suggests it to the user.
     * @param uploadItem
     */
    private void checkNearbyPlaces(final UploadItem uploadItem) {
        final Disposable checkNearbyPlaces = Maybe.fromCallable(() -> repository
                .checkNearbyPlaces(uploadItem.getGpsCoords().getDecLatitude(),
                        uploadItem.getGpsCoords().getDecLongitude()))
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(place -> {
                        if (place != null) {
                            view.onNearbyPlaceFound(uploadItem, place);
                        }
                    },
                    throwable -> Timber.e(throwable, "Error occurred in processing images"));
            compositeDisposable.add(checkNearbyPlaces);
    }

    /**
     * Checks if the image has a location. Displays a dialog alerting user that no
     * location has been to added to the image and asking them to add one, if location was not
     * removed by the user
     *
     * @param uploadItemIndex Index of the uploadItem which has no location
     * @param inAppPictureLocation In app picture location (if any)
     * @param hasUserRemovedLocation True if user has removed location from the image
     */
    @Override
    public void displayLocDialog(int uploadItemIndex, LatLng inAppPictureLocation,
        boolean hasUserRemovedLocation) {
        final List<UploadItem> uploadItems = repository.getUploads();
        UploadItem uploadItem = uploadItems.get(uploadItemIndex);
        if (uploadItem.getGpsCoords().getDecimalCoords() == null && inAppPictureLocation == null
            && !hasUserRemovedLocation) {
            final Runnable onSkipClicked = () -> {
                verifyCaptionQuality(uploadItem);
            };
            view.displayAddLocationDialog(onSkipClicked);
        } else {
            verifyCaptionQuality(uploadItem);
        }
    }

    /**
     * Verifies the image's caption and calls function to handle the result
     *
     * @param uploadItem UploadItem whose caption is checked
     */
    private void verifyCaptionQuality(UploadItem uploadItem) {
        view.showProgress(true);
        compositeDisposable.add(
            repository
                .getCaptionQuality(uploadItem)
                .observeOn(mainThreadScheduler)
                .subscribe(capResult -> {
                        view.showProgress(false);
                        handleCaptionResult(capResult, uploadItem);
                    },
                    throwable -> {
                        view.showProgress(false);
                        if (throwable instanceof UnknownHostException) {
                            view.showConnectionErrorPopupForCaptionCheck();
                        } else {
                            view.showMessage("" + throwable.getLocalizedMessage(),
                                R.color.color_error);
                        }
                        Timber.e(throwable, "Error occurred while handling image");
                    })
        );
    }

    /**
     * Handles image's caption results and shows dialog if necessary
     *
     * @param errorCode Error code of the UploadItem
     * @param uploadItem UploadItem whose caption is checked
     */
    public void handleCaptionResult(Integer errorCode, UploadItem uploadItem) {
        // If errorCode is empty caption show message
        if (errorCode == EMPTY_CAPTION) {
            Timber.d("Captions are empty. Showing toast");
            view.showMessage(R.string.add_caption_toast, R.color.color_error);
        }

        // If image with same file name exists check the bit in errorCode is set or not
        if ((errorCode & FILE_NAME_EXISTS) != 0) {
            Timber.d("Trying to show duplicate picture popup");
            view.showDuplicatePicturePopup(uploadItem);
        }

        // If caption is not duplicate or user still wants to upload it
        if (errorCode == IMAGE_OK) {
            Timber.d("Image captions are okay or user still wants to upload it");
            view.onImageValidationSuccess();
        }
    }


    /**
     * Copies the caption and description of the current item to the subsequent media
     *
     * @param indexInViewFlipper
     */
    @Override
    public void copyTitleAndDescriptionToSubsequentMedia(int indexInViewFlipper) {
      for(int i = indexInViewFlipper+1; i < repository.getCount(); i++){
        final UploadItem subsequentUploadItem = repository.getUploads().get(i);
        subsequentUploadItem.setMediaDetails(deepCopy(repository.getUploads().get(indexInViewFlipper).getUploadMediaDetails()));
      }
    }

  /**
   * Fetches and set the caption and description of the item
   *
   * @param indexInViewFlipper
   */
  @Override
  public void fetchTitleAndDescription(int indexInViewFlipper) {
    final UploadItem currentUploadItem = repository.getUploads().get(indexInViewFlipper);
    view.updateMediaDetails(currentUploadItem.getUploadMediaDetails());
  }

  @NotNull
  private List<UploadMediaDetail> deepCopy(List<UploadMediaDetail> uploadMediaDetails) {
    final ArrayList<UploadMediaDetail> newList = new ArrayList<>();
    for (UploadMediaDetail uploadMediaDetail : uploadMediaDetails) {
      newList.add(uploadMediaDetail.javaCopy());
    }
    return newList;
  }

  @Override
  public void useSimilarPictureCoordinates(ImageCoordinates imageCoordinates, int uploadItemIndex) {
    repository.useSimilarPictureCoordinates(imageCoordinates, uploadItemIndex);
  }

  @Override
  public void onMapIconClicked(int indexInViewFlipper) {
    view.showExternalMap(repository.getUploads().get(indexInViewFlipper));
  }

  @Override
  public void onEditButtonClicked(int indexInViewFlipper){
      view.showEditActivity(repository.getUploads().get(indexInViewFlipper));
  }

  /**
   * Updates the information regarding the specified place for uploads
   * when the user confirms the suggested nearby place.
   *
   * @param place The place to be associated with the uploads.
   */
  @Override
  public void onUserConfirmedUploadIsOfPlace(Place place) {
      final List<UploadItem> uploads = repository.getUploads();
      for (UploadItem uploadItem : uploads) {
          uploadItem.setPlace(place);
          final List<UploadMediaDetail> uploadMediaDetails = uploadItem.getUploadMediaDetails();
          // Update UploadMediaDetail object for this UploadItem
          uploadMediaDetails.set(0, new UploadMediaDetail(place));
      }
      // Now that all UploadItems and their associated UploadMediaDetail objects have been updated,
      // update the view with the modified media details of the first upload item
      view.updateMediaDetails(uploads.get(0).getUploadMediaDetails());
      UploadActivity.setUploadIsOfAPlace(true);
  }


    /**
     * Calculates the image quality
     *
     * @param uploadItemIndex      Index of the UploadItem whose quality is to be checked
     * @param inAppPictureLocation In app picture location (if any)
     * @param activity             Context reference
     * @return true if no internal error occurs, else returns false
     */
    @Override
    public boolean getImageQuality(int uploadItemIndex, LatLng inAppPictureLocation,
        Activity activity) {
        final List<UploadItem> uploadItems = repository.getUploads();
        view.showProgress(true);
        if (uploadItems.size() == 0) {
            view.showProgress(false);
            // No internationalization required for this error message because it's an internal error.
            view.showMessage(
                "Internal error: Zero upload items received by the Upload Media Detail Fragment. Sorry, please upload again.",
                R.color.color_error);
            return false;
        }
        UploadItem uploadItem = uploadItems.get(uploadItemIndex);
        compositeDisposable.add(
            repository
                .getImageQuality(uploadItem, inAppPictureLocation)
                .observeOn(mainThreadScheduler)
                .subscribe(imageResult -> {
                        storeImageQuality(imageResult, uploadItemIndex, activity, uploadItem);
                    },
                    throwable -> {
                        if (throwable instanceof UnknownHostException) {
                            view.showProgress(false);
                            view.showConnectionErrorPopup();
                        } else {
                            view.showMessage("" + throwable.getLocalizedMessage(),
                                R.color.color_error);
                        }
                        Timber.e(throwable, "Error occurred while handling image");
                    })
        );
        return true;
    }

    /**
     * Stores the image quality in JSON format in SharedPrefs
     *
     * @param imageResult     Image quality
     * @param uploadItemIndex Index of the UploadItem whose quality is calculated
     * @param activity        Context reference
     * @param uploadItem      UploadItem whose quality is to be checked
     */
    private void storeImageQuality(Integer imageResult, int uploadItemIndex, Activity activity,
        UploadItem uploadItem) {
        BasicKvStore store = new BasicKvStore(activity,
            UploadActivity.storeNameForCurrentUploadImagesSize);
        String value = store.getString(keyForCurrentUploadImageQualities, null);
        JSONObject jsonObject;
        try {
            if (value != null) {
                jsonObject = new JSONObject(value);
            } else {
                jsonObject = new JSONObject();
            }
            jsonObject.put("UploadItem" + uploadItemIndex, imageResult);
            store.putString(keyForCurrentUploadImageQualities, jsonObject.toString());
        } catch (Exception e) {
        }

        if (uploadItemIndex == 0) {
            if (!isBatteryDialogShowing && !isCategoriesDialogShowing) {
                // if battery-optimisation dialog is not being shown, call checkImageQuality
                checkImageQuality(uploadItem, uploadItemIndex);
            } else {
                view.showProgress(false);
            }
        }
    }

    /**
     * Used to check image quality from stored qualities and display dialogs
     *
     * @param uploadItem UploadItem whose quality is to be checked
     * @param index      Index of the UploadItem whose quality is to be checked
     */
    @Override
    public void checkImageQuality(UploadItem uploadItem, int index) {
        if ((uploadItem.getImageQuality() != IMAGE_OK) && (uploadItem.getImageQuality()
            != IMAGE_KEEP)) {
            BasicKvStore store = new BasicKvStore(activity,
                UploadActivity.storeNameForCurrentUploadImagesSize);
            String value = store.getString(keyForCurrentUploadImageQualities, null);
            JSONObject jsonObject;
            try {
                if (value != null) {
                    jsonObject = new JSONObject(value);
                } else {
                    jsonObject = new JSONObject();
                }
                Integer imageQuality = (int) jsonObject.get("UploadItem" + index);
                view.showProgress(false);
                if (imageQuality == IMAGE_OK) {
                    uploadItem.setHasInvalidLocation(false);
                    uploadItem.setImageQuality(imageQuality);
                } else {
                    handleBadImage(imageQuality, uploadItem, index);
                }
            } catch (Exception e) {
            }
        }
    }

    /**
     * Updates the image qualities stored in JSON, whenever an image is deleted
     *
     * @param size Size of uploadableFiles
     * @param index Index of the UploadItem which was deleted
     */
    @Override
    public void updateImageQualitiesJSON(int size, int index) {
        BasicKvStore store = new BasicKvStore(activity,
            UploadActivity.storeNameForCurrentUploadImagesSize);
        String value = store.getString(keyForCurrentUploadImageQualities, null);
        JSONObject jsonObject;
        try {
            if (value != null) {
                jsonObject = new JSONObject(value);
            } else {
                jsonObject = new JSONObject();
            }
            for (int i = index; i < (size - 1); i++) {
                jsonObject.put("UploadItem" + i, jsonObject.get("UploadItem" + (i + 1)));
            }
            jsonObject.remove("UploadItem" + (size - 1));
            store.putString(keyForCurrentUploadImageQualities, jsonObject.toString());
        } catch (Exception e) {
        }
    }

    /**
     * Handles bad pictures, like too dark, already on wikimedia, downloaded from internet
     *
     * @param errorCode Error code of the bad image quality
     * @param uploadItem UploadItem whose quality is bad
     * @param index Index of item whose quality is bad
     */
    public void handleBadImage(Integer errorCode,
        UploadItem uploadItem, int index) {
        Timber.d("Handle bad picture with error code %d", errorCode);
        if (errorCode >= 8) { // If location of image and nearby does not match
            uploadItem.setHasInvalidLocation(true);
        }

        // If image has some other problems, show popup accordingly
        if (errorCode != EMPTY_CAPTION && errorCode != FILE_NAME_EXISTS) {
            showBadImagePopup(errorCode, index, activity, uploadItem);
        }

    }

    /**
     * Shows a dialog describing the potential problems in the current image
     *
     * @param errorCode  Has the potential problems in the current image
     * @param index      Index of the UploadItem which has problems
     * @param activity   Context reference
     * @param uploadItem UploadItem which has problems
     */
    public void showBadImagePopup(Integer errorCode,
        int index, Activity activity, UploadItem uploadItem) {
        String errorMessageForResult = getErrorMessageForResult(activity, errorCode);
        if (!StringUtils.isBlank(errorMessageForResult)) {
            DialogUtil.showAlertDialog(activity,
                activity.getString(R.string.upload_problem_image),
                errorMessageForResult,
                activity.getString(R.string.upload),
                activity.getString(R.string.cancel),
                () -> {
                    view.showProgress(false);
                    uploadItem.setImageQuality(IMAGE_OK);
                },
                () -> {
                    presenterCallback.deletePictureAtIndex(index);
                }
            ).setCancelable(false);
        } else {
        }
        //If the error message is null, we will probably not show anything
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
