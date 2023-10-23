package fr.free.nrw.commons.upload.mediaDetails;

import static android.app.Activity.RESULT_OK;
import static fr.free.nrw.commons.utils.ActivityUtils.startActivityWithFlags;
import static fr.free.nrw.commons.utils.ImageUtils.FILE_NAME_EXISTS;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.github.chrisbanes.photoview.PhotoView;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import fr.free.nrw.commons.LocationPicker.LocationPicker;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.edit.EditActivity;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
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
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.R.drawable.*;
import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

public class UploadMediaDetailFragment extends UploadBaseFragment implements
    UploadMediaDetailsContract.View, UploadMediaDetailAdapter.EventListener {

    private static final int REQUEST_CODE = 1211;
    private static final int REQUEST_CODE_FOR_EDIT_ACTIVITY = 1212;

    /**
     * A key for applicationKvStore. By this key we can retrieve the location of last UploadItem ex.
     * 12.3433,54.78897 from applicationKvStore.
     */
    public static final String LAST_LOCATION = "last_location_while_uploading";
    public static final String LAST_ZOOM = "last_zoom_level_while_uploading";
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.ib_map)
    AppCompatImageButton ibMap;
    @BindView(R.id.ib_expand_collapse)
    AppCompatImageButton ibExpandCollapse;
    @BindView(R.id.ll_container_media_detail)
    LinearLayout llContainerMediaDetail;
    @BindView(R.id.rv_descriptions)
    RecyclerView rvDescriptions;
    @BindView(R.id.backgroundImage)
    PhotoView photoViewBackgroundImage;
    @BindView(R.id.btn_next)
    AppCompatButton btnNext;
    @BindView(R.id.btn_previous)
    AppCompatButton btnPrevious;
    @BindView(R.id.edit_image)
    AppCompatButton editImage;
    @BindView(R.id.tooltip)
    ImageView tooltip;

    private UploadMediaDetailAdapter uploadMediaDetailAdapter;
    @BindView(R.id.btn_copy_subsequent_media)
    AppCompatButton btnCopyToSubsequentMedia;

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

    private UploadMediaDetailFragmentCallback callback;

    public void setCallback(UploadMediaDetailFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setImageTobeUploaded(UploadableFile uploadableFile, Place place,
        LatLng inAppPictureLocation) {
        this.uploadableFile = uploadableFile;
        this.place = place;
        this.inAppPictureLocation = inAppPictureLocation;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
        @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_upload_media_detail_fragment, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
        if (callback != null) {
            init();
        }
    }

    private void init() {
        tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
            callback.getTotalNumberOfSteps(), getString(R.string.media_detail_step_title)));
        tooltip.setOnClickListener(
            v -> showInfoAlert(R.string.media_detail_step_title, R.string.media_details_tooltip));
        initPresenter();
        presenter.receiveImage(uploadableFile, place, inAppPictureLocation);
        initRecyclerView();

        if (callback.getIndexInViewFlipper(this) == 0) {
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
        }

        // If the image EXIF data contains the location, show the map icon with a green tick
        if (inAppPictureLocation != null ||
                (uploadableFile != null && uploadableFile.hasLocation())) {
            Drawable mapTick = getResources().getDrawable(R.drawable.ic_map_tick_white_24dp);
            ibMap.setImageDrawable(mapTick);
        } else {
            // Otherwise, show the map icon with a red question mark
            Drawable mapQuestionMark =
                getResources().getDrawable(R.drawable.ic_map_question_white_24dp);
            ibMap.setImageDrawable(mapQuestionMark);
        }

        //If this is the last media, we have nothing to copy, lets not show the button
        if (callback.getIndexInViewFlipper(this) == callback.getTotalNumberOfSteps() - 4) {
            btnCopyToSubsequentMedia.setVisibility(View.GONE);
        } else {
            btnCopyToSubsequentMedia.setVisibility(View.VISIBLE);
        }

        attachImageViewScaleChangeListener();
    }

    /**
     * Attaches the scale change listener to the image view
     */
    private void attachImageViewScaleChangeListener() {
        photoViewBackgroundImage.setOnScaleChangeListener(
            (scaleFactor, focusX, focusY) -> {
                //Whenever the uses plays with the image, lets collapse the media detail container
                expandCollapseLlMediaDetail(false);
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
        uploadMediaDetailAdapter = new UploadMediaDetailAdapter(
            defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, ""), recentLanguagesDao);
        uploadMediaDetailAdapter.setCallback(this::showInfoAlert);
        uploadMediaDetailAdapter.setEventListener(this);
        rvDescriptions.setLayoutManager(new LinearLayoutManager(getContext()));
        rvDescriptions.setAdapter(uploadMediaDetailAdapter);
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

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        boolean isValidUploads = presenter.verifyImageQuality(callback.getIndexInViewFlipper(this), inAppPictureLocation);
        if (!isValidUploads) {
            startActivityWithFlags(
                getActivity(), MainActivity.class, Intent.FLAG_ACTIVITY_CLEAR_TOP,
                Intent.FLAG_ACTIVITY_SINGLE_TOP);
        }
    }

    @OnClick(R.id.btn_previous)
    public void onPreviousButtonClicked() {
        callback.onPreviousButtonClicked(callback.getIndexInViewFlipper(this));
    }

    @OnClick(R.id.btn_add_description)
    public void onButtonAddDescriptionClicked() {
        UploadMediaDetail uploadMediaDetail = new UploadMediaDetail();
        uploadMediaDetail.setManuallyAdded(true);//This was manually added by the user
        uploadMediaDetailAdapter.addDescription(uploadMediaDetail);
        rvDescriptions.smoothScrollToPosition(uploadMediaDetailAdapter.getItemCount()-1);
    }

    @OnClick(R.id.edit_image)
    public void onEditButtonClicked() {
        presenter.onEditButtonClicked(callback.getIndexInViewFlipper(this));
    }
    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates) {
        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        newFragment.setCallback(new SimilarImageDialogFragment.Callback() {
            @Override
            public void onPositiveResponse() {
                Timber.d("positive response from similar image fragment");
                presenter.useSimilarPictureCoordinates(similarImageCoordinates, callback.getIndexInViewFlipper(UploadMediaDetailFragment.this));

                // set the description text when user selects to use coordinate from the other image
                // which was taken within 20s
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

    @Override
    public void onImageProcessed(UploadItem uploadItem, Place place) {
        photoViewBackgroundImage.setImageURI(uploadItem.getMediaUri());
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
        if (callback.getIndexInViewFlipper(this) == 0) {
            if (UploadActivity.nearbyPopupAnswers.containsKey(nearbyPlace)) {
                final boolean response = UploadActivity.nearbyPopupAnswers.get(nearbyPlace);
                if (response) {
                    presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace,
                        callback.getIndexInViewFlipper(this));
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
                presenter.onUserConfirmedUploadIsOfPlace(place, callback.getIndexInViewFlipper(this));
            },
            () -> {
                UploadActivity.nearbyPopupAnswers.put(place, false);
            },
            customLayout, true);
    }

    @Override
    public void showProgress(boolean shouldShow) {
        callback.showProgress(shouldShow);
    }

    @Override
    public void onImageValidationSuccess() {
        callback.onNextButtonClicked(callback.getIndexInViewFlipper(this));
    }

    /**
     * This method gets called whenever the next/previous button is pressed
     */
    @Override
    protected void onBecameVisible() {
        super.onBecameVisible();
        presenter.fetchTitleAndDescription(callback.getIndexInViewFlipper(this));
        if (showNearbyFound) {
            if (UploadActivity.nearbyPopupAnswers.containsKey(nearbyPlace)) {
                final boolean response = UploadActivity.nearbyPopupAnswers.get(nearbyPlace);
                if (response) {
                    presenter.onUserConfirmedUploadIsOfPlace(nearbyPlace,
                        callback.getIndexInViewFlipper(this));
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
            onNextButtonClicked();
        }
    }

    @Override
    public void showBadImagePopup(Integer errorCode,
        UploadItem uploadItem) {
        String errorMessageForResult = getErrorMessageForResult(getContext(), errorCode);
        if (!StringUtils.isBlank(errorMessageForResult)) {
            DialogUtil.showAlertDialog(getActivity(),
                getString(R.string.upload_problem_image),
                errorMessageForResult,
                getString(R.string.upload),
                getString(R.string.cancel),
                () -> {
                    /*
                        User skipped the warning of low quality image, so we call
                        onImageValidationSuccess rather than onNextButtonClicked to avoid showing
                        other warning popups again.
                    */

                    // validate image only when same file name error does not occur
                    // show the same file name error if exists.
                    if ((errorCode & FILE_NAME_EXISTS) == 0) {
                        uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                        onImageValidationSuccess();
                    }
                },
                () -> deleteThisPicture()
            );
        }
        //If the error message is null, we will probably not show anything
    }

    @Override
    public void showConnectionErrorPopup() {
        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.upload_connection_error_alert_title),
            getString(R.string.upload_connection_error_alert_detail), getString(R.string.ok),
            () -> {}, true);
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
                .defaultLocation(new CameraPosition.Builder()
                    .target(
                        new com.mapbox.mapboxsdk.geometry.LatLng(defaultLatitude, defaultLongitude))
                    .zoom(defaultZoom).build())
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
                .defaultLocation(new CameraPosition.Builder()
                    .target(
                        new com.mapbox.mapboxsdk.geometry.LatLng(defaultLatitude, defaultLongitude))
                    .zoom(defaultZoom).build())
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

                final String latitude = String.valueOf(cameraPosition.target.getLatitude());
                final String longitude = String.valueOf(cameraPosition.target.getLongitude());
                final double zoom = cameraPosition.zoom;

                editLocation(latitude, longitude, zoom);
                /*
                       If isMissingLocationDialog is true, it means that the user has already tapped the
                       "Next" button, so go directly to the next step.
                 */
                if (isMissingLocationDialog) {
                    isMissingLocationDialog = false;
                    onNextButtonClicked();
                }
            }
        }
        if (requestCode == REQUEST_CODE_FOR_EDIT_ACTIVITY && resultCode == RESULT_OK) {
            String result = data.getStringExtra("editedImageFilePath");

            if (Objects.equals(result, "Error")) {
                Timber.e("Error in rotating image");
                return;
            }
            try {
                photoViewBackgroundImage.setImageURI(Uri.fromFile(new File(result)));
                editableUploadItem.setContentUri(Uri.fromFile(new File(result)));
                callback.changeThumbnail(callback.getIndexInViewFlipper(this),
                    result);
            } catch (Exception e) {
                Timber.e(e);
            }
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
        Drawable mapTick = getResources().getDrawable(R.drawable.ic_map_tick_white_24dp);
        ibMap.setImageDrawable(mapTick);

        Toast.makeText(getContext(), "Location Updated", Toast.LENGTH_LONG).show();

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

    private void deleteThisPicture() {
        callback.deletePictureAtIndex(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        presenter.onDetachView();
    }

    @OnClick(R.id.rl_container_title)
    public void onRlContainerTitleClicked() {
        expandCollapseLlMediaDetail(!isExpanded);
    }

    /**
     * show hide media detail based on
     * @param shouldExpand
     */
    private void expandCollapseLlMediaDetail(boolean shouldExpand){
        llContainerMediaDetail.setVisibility(shouldExpand ? View.VISIBLE : View.GONE);
        isExpanded = !isExpanded;
        ibExpandCollapse.setRotation(ibExpandCollapse.getRotation() + 180);
    }

    @OnClick(R.id.ib_map) public void onIbMapClicked() {
        presenter.onMapIconClicked(callback.getIndexInViewFlipper(this));
    }

    @Override
    public void onPrimaryCaptionTextChange(boolean isNotEmpty) {
        btnCopyToSubsequentMedia.setEnabled(isNotEmpty);
        btnCopyToSubsequentMedia.setClickable(isNotEmpty);
        btnCopyToSubsequentMedia.setAlpha(isNotEmpty ? 1.0f : 0.5f);
        btnNext.setEnabled(isNotEmpty);
        btnNext.setClickable(isNotEmpty);
        btnNext.setAlpha(isNotEmpty ? 1.0f : 0.5f);
    }

    public interface UploadMediaDetailFragmentCallback extends Callback {

        void deletePictureAtIndex(int index);

        void changeThumbnail(int index, String uri);
    }


    @OnClick(R.id.btn_copy_subsequent_media)
    public void onButtonCopyTitleDescToSubsequentMedia(){
        presenter.copyTitleAndDescriptionToSubsequentMedia(callback.getIndexInViewFlipper(this));
        Toast.makeText(getContext(), getResources().getString(R.string.copied_successfully), Toast.LENGTH_SHORT).show();
    }

}
