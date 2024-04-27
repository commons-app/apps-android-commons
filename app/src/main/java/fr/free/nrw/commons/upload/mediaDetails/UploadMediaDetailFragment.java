package fr.free.nrw.commons.upload.mediaDetails;

import static android.app.Activity.RESULT_OK;
import static fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import androidx.recyclerview.widget.LinearLayoutManager;
import fr.free.nrw.commons.CameraPosition;
import fr.free.nrw.commons.LocationPicker.LocationPicker;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.databinding.FragmentUploadMediaDetailFragmentBinding;
import fr.free.nrw.commons.edit.EditActivity;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.BasicKvStore;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadItem;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class UploadMediaDetailFragment extends UploadBaseFragment implements
    UploadMediaDetailsContract.View, UploadMediaDetailAdapter.EventListener {

    private static final int REQUEST_CODE = 1211;
    private static final int REQUEST_CODE_FOR_EDIT_ACTIVITY = 1212;
    private static final int REQUEST_CODE_FOR_VOICE_INPUT = 1213;

    public static Activity activity ;

    private int indexOfFragment;

    /**
     * A key for applicationKvStore. By this key we can retrieve the location of last UploadItem ex.
     * 12.3433,54.78897 from applicationKvStore.
     */
    public static final String LAST_LOCATION = "last_location_while_uploading";
    public static final String LAST_ZOOM = "last_zoom_level_while_uploading";


    public static final String UPLOADABLE_FILE = "uploadable_file";

    public static final String UPLOAD_MEDIA_DETAILS = "upload_media_detail_adapter";

    /**
     * True when user removes location from the current image
     */
    private boolean hasUserRemovedLocation;


    private UploadMediaDetailAdapter uploadMediaDetailAdapter;

    @Inject
    UploadMediaDetailsContract.UserActionListener presenter;

    @Inject
    @Named("default_preferences")
    JsonKvStore defaultKvStore;

    @Inject
    RecentLanguagesDao recentLanguagesDao;

    private UploadableFile uploadableFile;
    private Place place;

    private boolean isExpanded = true;

    /**
     * True if location is added via the "missing location" popup dialog (which appears after
     * tapping "Next" if the picture has no geographical coordinates).
     */
    private boolean isMissingLocationDialog;

    /**
     * showNearbyFound will be true, if any nearby location found that needs pictures and the nearby
     * popup is yet to be shown Used to show and check if the nearby found popup is already shown
     */
    private boolean showNearbyFound;

    /**
     * nearbyPlace holds the detail of nearby place that need pictures, if any found
     */
    private Place nearbyPlace;
    private UploadItem uploadItem;
    /**
     * inAppPictureLocation: use location recorded while using the in-app camera if device camera
     * does not record it in the EXIF
     */
    private LatLng inAppPictureLocation;
    /**
     * editableUploadItem : Storing the upload item before going to update the coordinates
     */
    private UploadItem editableUploadItem;

    private BasicKvStore basicKvStore;
    
    private final String keyForShowingAlertDialog = "isNoNetworkAlertDialogShowing";

    private UploadMediaDetailFragmentCallback callback;

    private FragmentUploadMediaDetailFragmentBinding binding;

    public void setCallback(UploadMediaDetailFragmentCallback callback) {
        this.callback = callback;
        UploadMediaPresenter.presenterCallback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if(savedInstanceState!=null && uploadableFile==null) {
            uploadableFile = savedInstanceState.getParcelable(UPLOADABLE_FILE);
        }

    }



    public void setImageToBeUploaded(UploadableFile uploadableFile, Place place,
        LatLng inAppPictureLocation) {
        this.uploadableFile = uploadableFile;
        this.place = place;
        this.inAppPictureLocation = inAppPictureLocation;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        binding = FragmentUploadMediaDetailFragmentBinding.inflate(inflater, container, false);
        return binding.getRoot();

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        activity = getActivity();
        basicKvStore = new BasicKvStore(activity, "CurrentUploadImageQualities");

        if (callback != null) {
            indexOfFragment = callback.getIndexInViewFlipper(this);
            init();
        }

        if(savedInstanceState!=null){
                if(uploadMediaDetailAdapter.getItems().size()==0 && callback != null){
                    uploadMediaDetailAdapter.setItems(savedInstanceState.getParcelableArrayList(UPLOAD_MEDIA_DETAILS));
                    presenter.setUploadMediaDetails(uploadMediaDetailAdapter.getItems(),
                        indexOfFragment);
                }
        }

        try {
            if(!presenter.getImageQuality(indexOfFragment, inAppPictureLocation, getActivity())) {
                startActivityWithFlags(
                getActivity(), MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
            }
        } catch (Exception e) {
        }

    }

    private void init() {
        if (binding == null) {
            return;
        }
        binding.tvTitle.setText(getString(R.string.step_count, (indexOfFragment + 1),
            callback.getTotalNumberOfSteps(), getString(R.string.media_detail_step_title)));
        binding.tooltip.setOnClickListener(
            v -> showInfoAlert(R.string.media_detail_step_title, R.string.media_details_tooltip));
        initPresenter();
        presenter.receiveImage(uploadableFile, place, inAppPictureLocation);
        initRecyclerView();

        if (indexOfFragment == 0) {
            binding.btnPrevious.setEnabled(false);
            binding.btnPrevious.setAlpha(0.5f);
        } else {
            binding.btnPrevious.setEnabled(true);
            binding.btnPrevious.setAlpha(1.0f);
        }

        // If the image EXIF data contains the location, show the map icon with a green tick
        if (inAppPictureLocation != null ||
                (uploadableFile != null && uploadableFile.hasLocation())) {
            Drawable mapTick = getResources().getDrawable(R.drawable.ic_map_available_20dp);
            binding.locationImageView.setImageDrawable(mapTick);
            binding.locationTextView.setText(R.string.edit_location);
        } else {
            // Otherwise, show the map icon with a red question mark
            Drawable mapQuestionMark =
                getResources().getDrawable(R.drawable.ic_map_not_available_20dp);
            binding.locationImageView.setImageDrawable(mapQuestionMark);
            binding.locationTextView.setText(R.string.add_location);
        }

        //If this is the last media, we have nothing to copy, lets not show the button
        if (indexOfFragment == callback.getTotalNumberOfSteps() - 4) {
            binding.btnCopySubsequentMedia.setVisibility(View.GONE);
        } else {
            binding.btnCopySubsequentMedia.setVisibility(View.VISIBLE);
        }

        binding.btnNext.setOnClickListener(v -> onNextButtonClicked());
        binding.btnPrevious.setOnClickListener(v -> onPreviousButtonClicked());
        binding.llEditImage.setOnClickListener(v -> onEditButtonClicked());
        binding.llContainerTitle.setOnClickListener(v -> onLlContainerTitleClicked());
        binding.llLocationStatus.setOnClickListener(v -> onIbMapClicked());
        binding.btnCopySubsequentMedia.setOnClickListener(v -> onButtonCopyTitleDescToSubsequentMedia());


        attachImageViewScaleChangeListener();
    }

    /**
     * Attaches the scale change listener to the image view
     */
    private void attachImageViewScaleChangeListener() {
        binding.backgroundImage.setOnScaleChangeListener(
            (scaleFactor, focusX, focusY) -> {
                //Whenever the uses plays with the image, lets collapse the media detail container
                //only if it is not already collapsed, which resolves flickering of arrow
                if (isExpanded) {
                    expandCollapseLlMediaDetail(false);
                }
            });
    }

    /**
     * attach the presenter with the view
     */
    private void initPresenter() {
        presenter.onAttachView(this);
    }

    /**
     * init the description recycler veiw and caption recyclerview
     */
    private void initRecyclerView() {
        uploadMediaDetailAdapter = new UploadMediaDetailAdapter(this,
            defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, ""), recentLanguagesDao);
        uploadMediaDetailAdapter.setCallback(this::showInfoAlert);
        uploadMediaDetailAdapter.setEventListener(this);
        binding.rvDescriptions.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.rvDescriptions.setAdapter(uploadMediaDetailAdapter);
    }

    /**
     * show dialog with info
     * @param titleStringID
     * @param messageStringId
     */
    private void showInfoAlert(int titleStringID, int messageStringId) {
        DialogUtil.showAlertDialog(getActivity(), getString(titleStringID),
            getString(messageStringId), getString(android.R.string.ok), null, true);
    }


    public void onNextButtonClicked() {
        if (callback == null) {
            return;
        }
        presenter.displayLocDialog(indexOfFragment, inAppPictureLocation, hasUserRemovedLocation);
    }

    public void onPreviousButtonClicked() {
        if (callback == null) {
            return;
        }
        callback.onPreviousButtonClicked(indexOfFragment);
    }

    public void onEditButtonClicked() {
        presenter.onEditButtonClicked(indexOfFragment);
    }
    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates) {
        BasicKvStore basicKvStore = new BasicKvStore(getActivity(), "IsAnyImageCancelled");
        if (!basicKvStore.getBoolean("IsAnyImageCancelled", false)) {
            SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
            newFragment.setCallback(new SimilarImageDialogFragment.Callback() {
                @Override
                public void onPositiveResponse() {
                    Timber.d("positive response from similar image fragment");
                    presenter.useSimilarPictureCoordinates(similarImageCoordinates,
                        indexOfFragment);

                    // set the description text when user selects to use coordinate from the other image
                    // which was taken within 120s
                    // fixing: https://github.com/commons-app/apps-android-commons/issues/4700
                    uploadMediaDetailAdapter.getItems().get(0).setDescriptionText(
                        getString(R.string.similar_coordinate_description_auto_set));
                    updateMediaDetails(uploadMediaDetailAdapter.getItems());
                }

                @Override
                public void onNegativeResponse() {
                    Timber.d("negative response from similar image fragment");
                }
            });
            Bundle args = new Bundle();
            args.putString("originalImagePath", originalFilePath);
            args.putString("possibleImagePath", possibleFilePath);
            newFragment.setArguments(args);
            newFragment.show(getChildFragmentManager(), "dialog");
        }
    }

    @Override
    public void onImageProcessed(UploadItem uploadItem, Place place) {
        if (binding == null) {
            return;
        }
        binding.backgroundImage.setImageURI(uploadItem.getMediaUri());
    }

    /**
     * Sets variables to Show popup if any nearby location needing pictures matches uploadable picture's GPS location
     * @param uploadItem
     * @param place
     */
    @Override
    public void onNearbyPlaceFound(UploadItem uploadItem, Place place) {
        nearbyPlace = place;
        this.uploadItem = uploadItem;
        showNearbyFound = true;
        if (callback == null) {
            return;
        }
        if (indexOfFragment == 0) {
            if (UploadActivity.nearbyPopupAnswers.containsKey(nearbyPlace)) {
                final boolean response = UploadActivity.nearbyPopupAnswers.get(nearbyPlace);
                if (response) {
                    if (callback != null) {
                        presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace,
                            indexOfFragment);
                    }
                }
            } else {
                showNearbyPlaceFound(nearbyPlace);
            }
            showNearbyFound = false;
        }
    }

    /**
     * Shows nearby place found popup
     * @param place
     */
    @SuppressLint("StringFormatInvalid")
    // To avoid the unwanted lint warning that string 'upload_nearby_place_found_description' is not of a valid format
    private void showNearbyPlaceFound(Place place) {
        final View customLayout = getLayoutInflater().inflate(R.layout.custom_nearby_found, null);
        ImageView nearbyFoundImage = customLayout.findViewById(R.id.nearbyItemImage);
        nearbyFoundImage.setImageURI(uploadItem.getMediaUri());
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.upload_nearby_place_found_title),
            String.format(Locale.getDefault(),
                getString(R.string.upload_nearby_place_found_description),
                place.getName()),
            () -> {
                UploadActivity.nearbyPopupAnswers.put(place, true);
                presenter.onUserConfirmedUploadIsOfPlace(place, indexOfFragment);
            },
            () -> {
                UploadActivity.nearbyPopupAnswers.put(place, false);
            },
            customLayout, true);
    }

    @Override
    public void showProgress(boolean shouldShow) {
        if (callback == null) {
            return;
        }
        callback.showProgress(shouldShow);
    }

    @Override
    public void onImageValidationSuccess() {
        if (callback == null) {
            return;
        }
        callback.onNextButtonClicked(indexOfFragment);
    }

    /**
     * This method gets called whenever the next/previous button is pressed
     */
    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        if (callback == null) {
            return;
        }
        presenter.fetchTitleAndDescription(indexOfFragment);
        if (showNearbyFound) {
            if (UploadActivity.nearbyPopupAnswers.containsKey(nearbyPlace)) {
                final boolean response = UploadActivity.nearbyPopupAnswers.get(nearbyPlace);
                if (response) {
                    presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace,
                        indexOfFragment);
                }
            } else {
                showNearbyPlaceFound(nearbyPlace);
            }
            showNearbyFound = false;
        }
    }

    @Override
    public void showMessage(int stringResourceId, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), stringResourceId);
    }

    @Override
    public void showMessage(String message, int colorResourceId) {
        ViewUtil.showLongToast(getContext(), message);
    }

    @Override
    public void showDuplicatePicturePopup(UploadItem uploadItem) {
        if (defaultKvStore.getBoolean("showDuplicatePicturePopup", true)) {
            String uploadTitleFormat = getString(R.string.upload_title_duplicate);
            View checkBoxView = View
                .inflate(getActivity(), R.layout.nearby_permission_dialog, null);
            CheckBox checkBox = (CheckBox) checkBoxView.findViewById(R.id.never_ask_again);
            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    defaultKvStore.putBoolean("showDuplicatePicturePopup", false);
                }
            });
            DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.duplicate_file_name),
                String.format(Locale.getDefault(),
                    uploadTitleFormat,
                    uploadItem.getFileName()),
                getString(R.string.upload),
                getString(R.string.cancel),
                () -> {
                    uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                    onImageValidationSuccess();
                }, null,
                checkBoxView,
                false);
        } else {
            uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
            onImageValidationSuccess();
        }
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Does nothing if there is network connectivity and then the user presses okay
     */
    @Override
    public void showConnectionErrorPopupForCaptionCheck() {
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.upload_connection_error_alert_title),
            getString(R.string.upload_connection_error_alert_detail),
            getString(R.string.ok),
            getString(R.string.cancel_upload),
            () -> {
                if (!NetworkUtils.isInternetConnectionEstablished(activity)) {
                    showConnectionErrorPopupForCaptionCheck();
                }
            },
            () -> {
                activity.finish();
            });
    }

    /**
     * Shows a dialog alerting the user that internet connection is required for upload process
     * Recalls UploadMediaPresenter.getImageQuality for all the next upload items,
     * if there is network connectivity and then the user presses okay
     */
    @Override
    public void showConnectionErrorPopup() {
        try {
            boolean FLAG_ALERT_DIALOG_SHOWING = basicKvStore.getBoolean(
                keyForShowingAlertDialog, false);
            if (!FLAG_ALERT_DIALOG_SHOWING) {
                basicKvStore.putBoolean(keyForShowingAlertDialog, true);
                DialogUtil.showAlertDialog(getActivity(),
                    getString(R.string.upload_connection_error_alert_title),
                    getString(R.string.upload_connection_error_alert_detail),
                    getString(R.string.ok),
                    getString(R.string.cancel_upload),
                    () -> {
                        basicKvStore.putBoolean(keyForShowingAlertDialog, false);
                        if (NetworkUtils.isInternetConnectionEstablished(activity)) {
                            int sizeOfUploads = basicKvStore.getInt(
                                UploadActivity.keyForCurrentUploadImagesSize);
                            for (int i = indexOfFragment; i < sizeOfUploads; i++) {
                                presenter.getImageQuality(i, inAppPictureLocation, activity);
                            }
                        } else {
                            showConnectionErrorPopup();
                        }
                    },
                    () -> {
                        basicKvStore.putBoolean(keyForShowingAlertDialog, false);
                        activity.finish();
                    },
                    null,
                    false
                );
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void showExternalMap(final UploadItem uploadItem) {
        goToLocationPickerActivity(uploadItem);
    }

    /**
     * Launches the image editing activity to edit the specified UploadItem.
     *
     * @param uploadItem The UploadItem to be edited.
     *
     * This method is called to start the image editing activity for a specific UploadItem.
     * It sets the UploadItem as the currently editable item, creates an intent to launch the
     * EditActivity, and passes the image file path as an extra in the intent. The activity
     * is started with a request code, allowing the result to be handled in onActivityResult.
     */
    @Override
    public void showEditActivity(UploadItem uploadItem) {
        editableUploadItem = uploadItem;
        Intent intent = new Intent(getContext(), EditActivity.class);
        intent.putExtra("image", uploadableFile.getFilePath().toString());
        startActivityForResult(intent, REQUEST_CODE_FOR_EDIT_ACTIVITY);
    }

    /**
     * Start Location picker activity. Show the location first then user can modify it by clicking
     * modify location button.
     * @param uploadItem current upload item
     */
    private void goToLocationPickerActivity(final UploadItem uploadItem) {

        editableUploadItem = uploadItem;
        double defaultLatitude = 37.773972;
        double defaultLongitude = -122.431297;
        double defaultZoom = 16.0;

        /* Retrieve image location from EXIF if present or
           check if user has provided location while using the in-app camera.
           Use location of last UploadItem if none of them is available */
        if (uploadItem.getGpsCoords() != null && uploadItem.getGpsCoords()
            .getDecLatitude() != 0.0 && uploadItem.getGpsCoords().getDecLongitude() != 0.0) {
            defaultLatitude = uploadItem.getGpsCoords()
                .getDecLatitude();
            defaultLongitude = uploadItem.getGpsCoords().getDecLongitude();
            defaultZoom = uploadItem.getGpsCoords().getZoomLevel();
            startActivityForResult(new LocationPicker.IntentBuilder()
                .defaultLocation(new CameraPosition(defaultLatitude,defaultLongitude,defaultZoom))
                .activityKey("UploadActivity")
                .build(getActivity()), REQUEST_CODE);
        } else {
            if (defaultKvStore.getString(LAST_LOCATION) != null) {
                final String[] locationLatLng
                    = defaultKvStore.getString(LAST_LOCATION).split(",");
                defaultLatitude = Double.parseDouble(locationLatLng[0]);
                defaultLongitude = Double.parseDouble(locationLatLng[1]);
            }
            if (defaultKvStore.getString(LAST_ZOOM) != null) {
                defaultZoom = Double.parseDouble(defaultKvStore.getString(LAST_ZOOM));
            }
            startActivityForResult(new LocationPicker.IntentBuilder()
                .defaultLocation(new CameraPosition(defaultLatitude,defaultLongitude,defaultZoom))
                .activityKey("NoLocationUploadActivity")
                .build(getActivity()), REQUEST_CODE);
        }
    }

    /**
     * Get the coordinates and update the existing coordinates.
     * @param requestCode code of request
     * @param resultCode code of result
     * @param data intent
     */
    @Override
    public void onActivityResult(final int requestCode, final int resultCode,
        @Nullable final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {

            assert data != null;
            final CameraPosition cameraPosition = LocationPicker.getCameraPosition(data);

            if (cameraPosition != null) {

                final String latitude = String.valueOf(cameraPosition.getLatitude());
                final String longitude = String.valueOf(cameraPosition.getLongitude());
                final double zoom = cameraPosition.getZoom();

                editLocation(latitude, longitude, zoom);
                // If isMissingLocationDialog is true, it means that the user has already tapped the
                // "Next" button, so go directly to the next step.
                if (isMissingLocationDialog) {
                    isMissingLocationDialog = false;
                    onNextButtonClicked();
                }
            } else {
                // If camera position is null means location is removed by the user
                removeLocation();
            }
        }
        if (requestCode == REQUEST_CODE_FOR_EDIT_ACTIVITY && resultCode == RESULT_OK) {
            String result = data.getStringExtra("editedImageFilePath");

            if (Objects.equals(result, "Error")) {
                Timber.e("Error in rotating image");
                return;
            }
            try {
                if (binding != null){
                    binding.backgroundImage.setImageURI(Uri.fromFile(new File(result)));
                }
                editableUploadItem.setContentUri(Uri.fromFile(new File(result)));
                callback.changeThumbnail(indexOfFragment,
                    result);
            } catch (Exception e) {
                Timber.e(e);
            }
        }
        else if (requestCode == REQUEST_CODE_FOR_VOICE_INPUT) {
            if (resultCode == RESULT_OK && data != null) {
                ArrayList<String> result = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
                uploadMediaDetailAdapter.handleSpeechResult(result.get(0));
            }else {
                Timber.e("Error %s", resultCode);
            }
        }
    }

    /**
     * Removes the location data from the image, by setting them to null
     */
    public void removeLocation() {
        editableUploadItem.getGpsCoords().setDecimalCoords(null);
        try {
            ExifInterface sourceExif = new ExifInterface(uploadableFile.getFilePath());
            String[] exifTags = {
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
            };

            for (String tag : exifTags) {
                sourceExif.setAttribute(tag, null);
            }
            sourceExif.saveAttributes();

            Drawable mapQuestion = getResources().getDrawable(R.drawable.ic_map_not_available_20dp);

            if (binding != null) {
                binding.locationImageView.setImageDrawable(mapQuestion);
                binding.locationTextView.setText(R.string.add_location);
            }

            editableUploadItem.getGpsCoords().setDecLatitude(0.0);
            editableUploadItem.getGpsCoords().setDecLongitude(0.0);
            editableUploadItem.getGpsCoords().setImageCoordsExists(false);
            hasUserRemovedLocation = true;

            Toast.makeText(getContext(), getString(R.string.location_removed), Toast.LENGTH_LONG)
                .show();
        } catch (Exception e) {
            Timber.d(e);
            Toast.makeText(getContext(), "Location could not be removed due to internal error",
                Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Update the old coordinates with new one
     * @param latitude new latitude
     * @param longitude new longitude
     */
    public void editLocation(final String latitude, final String longitude, final double zoom) {

        editableUploadItem.getGpsCoords().setDecLatitude(Double.parseDouble(latitude));
        editableUploadItem.getGpsCoords().setDecLongitude(Double.parseDouble(longitude));
        editableUploadItem.getGpsCoords().setDecimalCoords(latitude + "|" + longitude);
        editableUploadItem.getGpsCoords().setImageCoordsExists(true);
        editableUploadItem.getGpsCoords().setZoomLevel(zoom);

        // Replace the map icon using the one with a green tick
        Drawable mapTick = getResources().getDrawable(R.drawable.ic_map_available_20dp);

        if (binding != null) {
            binding.locationImageView.setImageDrawable(mapTick);
            binding.locationTextView.setText(R.string.edit_location);
        }

        Toast.makeText(getContext(), getString(R.string.location_updated), Toast.LENGTH_LONG).show();

    }

    @Override
    public void updateMediaDetails(List<UploadMediaDetail> uploadMediaDetails) {
        uploadMediaDetailAdapter.setItems(uploadMediaDetails);
        showNearbyFound =
            showNearbyFound && (
                uploadMediaDetails == null || uploadMediaDetails.isEmpty()
                    || listContainsEmptyDetails(
                    uploadMediaDetails));
    }

    /**
     * if the media details that come in here are empty
     * (empty caption AND empty description, with caption being the decider here)
     * this method allows usage of nearby place caption and description if any
     * else it takes the media details saved in prior for this picture
     * @param uploadMediaDetails saved media details,
     *                           ex: in case when "copy to subsequent media" button is clicked
     *                           for a previous image
     * @return boolean whether the details are empty or not
     */
    private boolean listContainsEmptyDetails(List<UploadMediaDetail> uploadMediaDetails) {
        for (UploadMediaDetail uploadDetail: uploadMediaDetails) {
            if (!TextUtils.isEmpty(uploadDetail.getCaptionText()) && !TextUtils.isEmpty(uploadDetail.getDescriptionText())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Showing dialog for adding location
     *
     * @param onSkipClicked proceed for verifying image quality
     */
    @Override
    public void displayAddLocationDialog(final Runnable onSkipClicked) {
        isMissingLocationDialog = true;
        DialogUtil.showAlertDialog(Objects.requireNonNull(getActivity()),
            getString(R.string.no_location_found_title),
            getString(R.string.no_location_found_message),
            getString(R.string.add_location),
            getString(R.string.skip_login),
            this::onIbMapClicked,
            onSkipClicked);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    public void onLlContainerTitleClicked() {
        expandCollapseLlMediaDetail(!isExpanded);
    }

    /**
     * show hide media detail based on
     * @param shouldExpand
     */
    private void expandCollapseLlMediaDetail(boolean shouldExpand){
        if (binding == null) {
            return;
        }
        binding.llContainerMediaDetail.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
        isExpanded = !isExpanded;
        binding.ibExpandCollapse.setRotation(binding.ibExpandCollapse.getRotation() + 180);
    }

    public void onIbMapClicked() {
        if (callback == null) {
            return;
        }
        presenter.onMapIconClicked(indexOfFragment);
    }

    @Override
    public void onPrimaryCaptionTextChange(boolean isNotEmpty) {
        if (binding == null) {
            return;
        }
        binding.btnCopySubsequentMedia.setEnabled(isNotEmpty);
        binding.btnCopySubsequentMedia.setClickable(isNotEmpty);
        binding.btnCopySubsequentMedia.setAlpha(isNotEmpty ? 1.0f : 0.5f);
        binding.btnNext.setEnabled(isNotEmpty);
        binding.btnNext.setClickable(isNotEmpty);
        binding.btnNext.setAlpha(isNotEmpty ? 1.0f : 0.5f);
    }

    /**
     * Adds new language item to RecyclerView
     */
    @Override
    public void addLanguage() {
        UploadMediaDetail uploadMediaDetail = new UploadMediaDetail();
        uploadMediaDetail.setManuallyAdded(true);//This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail);
        binding.rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.getItemCount()-1);
    }

    public interface UploadMediaDetailFragmentCallback extends Callback {

        void deletePictureAtIndex(int index);

        void changeThumbnail(int index, String uri);
    }


    public void onButtonCopyTitleDescToSubsequentMedia(){
        presenter.copyTitleAndDescriptionToSubsequentMedia(indexOfFragment);
        Toast.makeText(getContext(), getResources().getString(R.string.copied_successfully), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onSaveInstanceState(final Bundle outState) {
        super.onSaveInstanceState(outState);

        if(uploadableFile!=null){
            outState.putParcelable(UPLOADABLE_FILE,uploadableFile);
        }
        if(uploadMediaDetailAdapter!=null){
            outState.putParcelableArrayList(UPLOAD_MEDIA_DETAILS,
                (ArrayList<? extends Parcelable>) uploadMediaDetailAdapter.getItems());
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
