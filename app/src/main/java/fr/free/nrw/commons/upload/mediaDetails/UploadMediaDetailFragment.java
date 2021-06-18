package fr.free.nrw.commons.upload.mediaDetails;

import static android.app.Activity.RESULT_CANCELED;
import static android.app.Activity.RESULT_OK;
import static fr.free.nrw.commons.utils.ImageUtils.getErrorMessageForResult;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
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
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.settings.Prefs;
import fr.free.nrw.commons.upload.ImageCoordinates;
import fr.free.nrw.commons.upload.SimilarImageDialogFragment;
import fr.free.nrw.commons.upload.UploadBaseFragment;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadMediaDetailAdapter;
import fr.free.nrw.commons.upload.UploadItem;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ImageUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.commons.lang3.StringUtils;
import timber.log.Timber;

public class UploadMediaDetailFragment extends UploadBaseFragment implements
    UploadMediaDetailsContract.View, UploadMediaDetailAdapter.EventListener {

    private static final int REQUEST_CODE = 1211;
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

    private UploadableFile uploadableFile;
    private Place place;

    private boolean isExpanded = true;

    /**
     * showNearbyFound will be true, if any nearby location found that needs pictures and the nearby popup is yet to be shown
     * Used to show and check if the nearby found popup is already shown
     */
    private boolean showNearbyFound;

    /**
     * nearbyPlace holds the detail of nearby place that need pictures, if any found
     */
    private Place nearbyPlace;
    private UploadItem uploadItem;


    private UploadMediaDetailFragmentCallback callback;

    public void setCallback(UploadMediaDetailFragmentCallback callback) {
        this.callback = callback;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void setImageTobeUploaded(UploadableFile uploadableFile, Place place) {
        this.uploadableFile = uploadableFile;
        this.place = place;
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
        init();
    }

    private void init() {
        tvTitle.setText(getString(R.string.step_count, callback.getIndexInViewFlipper(this) + 1,
            callback.getTotalNumberOfSteps(), getString(R.string.media_detail_step_title)));
        tooltip.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showInfoAlert(R.string.media_detail_step_title, R.string.media_details_tooltip);
            }
        });
        initPresenter();
        presenter.receiveImage(uploadableFile, place);
        initRecyclerView();

        if (callback.getIndexInViewFlipper(this) == 0) {
            btnPrevious.setEnabled(false);
            btnPrevious.setAlpha(0.5f);
        } else {
            btnPrevious.setEnabled(true);
            btnPrevious.setAlpha(1.0f);
        }

        //If this is the last media, we have nothing to copy, lets not show the button
        if (callback.getIndexInViewFlipper(this) == callback.getTotalNumberOfSteps()-4) {
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
        uploadMediaDetailAdapter = new UploadMediaDetailAdapter(defaultKvStore.getString(Prefs.DESCRIPTION_LANGUAGE, ""));
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
        DialogUtil.showAlertDialog(getActivity(), getString(titleStringID), getString(messageStringId), getString(android.R.string.ok), null, true);
    }

    @OnClick(R.id.btn_next)
    public void onNextButtonClicked() {
        presenter.verifyImageQuality(callback.getIndexInViewFlipper(this));
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

    @Override
    public void showSimilarImageFragment(String originalFilePath, String possibleFilePath,
        ImageCoordinates similarImageCoordinates) {
        SimilarImageDialogFragment newFragment = new SimilarImageDialogFragment();
        newFragment.setCallback(new SimilarImageDialogFragment.Callback() {
            @Override
            public void onPositiveResponse() {
                Timber.d("positive response from similar image fragment");
                presenter.useSimilarPictureCoordinates(similarImageCoordinates, callback.getIndexInViewFlipper(UploadMediaDetailFragment.this));
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
        if(callback.getIndexInViewFlipper(this) == 0) {
            showNearbyPlaceFound(nearbyPlace);
            showNearbyFound = false;
        }
    }

    /**
     * Shows nearby place found popup
     * @param place
     */
    @SuppressLint("StringFormatInvalid") // To avoid the unwanted lint warning that string 'upload_nearby_place_found_description' is not of a valid format
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
                presenter.onUserConfirmedUploadIsOfPlace(place, callback.getIndexInViewFlipper(this));
            },
            () -> {

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
        if(showNearbyFound) {
            showNearbyPlaceFound(nearbyPlace);
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
                getString(R.string.duplicate_image_found),
                String.format(Locale.getDefault(),
                    uploadTitleFormat,
                    uploadItem.getFileName()),
                getString(R.string.upload),
                getString(R.string.cancel),
                () -> {
                    uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                    onNextButtonClicked();
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
                    uploadItem.setImageQuality(ImageUtils.IMAGE_KEEP);
                    onNextButtonClicked();
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

    @Override public void showMapWithImageCoordinates(boolean shouldShow) {
        ibMap.setVisibility(shouldShow ? View.VISIBLE : View.GONE);
    }

    @Override
    public void showExternalMap(UploadItem uploadItem) {

        DialogUtil.showAlertDialog(getActivity(),
            getString(R.string.upload_problem_image),
            "Choose as your requirement",
            "Show in map",
            "Edit location",
            this::showInMap,
            ()-> goToLocationPickerActivity(uploadItem)
        );

    }

    private void goToLocationPickerActivity(UploadItem uploadItem) {

        startActivityForResult(new LocationPicker.IntentBuilder()
            .defaultLocation(new CameraPosition.Builder()
                .target(new com.mapbox.mapboxsdk.geometry.LatLng(uploadItem.getGpsCoords().getDecLatitude(),
                    uploadItem.getGpsCoords().getDecLongitude()))
                .zoom(16).build())
            .build(getActivity()),REQUEST_CODE);
    }

    /**
     * Get the coordinates and update the existing coordinates.
     * @param requestCode
     * @param resultCode
     * @param data
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

                editLocation(latitude, longitude);

            }
        } else if (resultCode == RESULT_CANCELED) {
            Toast.makeText(getContext(),"Unable to get coords",Toast.LENGTH_LONG).show();
        }
    }

    public void editLocation(String latitude, String longitude){

        uploadItem.getGpsCoords().setDecLatitude(Double.parseDouble(latitude));
        uploadItem.getGpsCoords().setDecLongitude(Double.parseDouble(longitude));
        uploadItem.getGpsCoords().setDecimalCoords(latitude+"|"+longitude);
        uploadItem.getGpsCoords().setImageCoordsExists(true);
        Log.d("abba","1st am I"+uploadItem.getGpsCoords().getDecLatitude());
    }

    public void showInMap(){
        Utils.handleGeoCoordinates(getContext(),
            new LatLng(uploadItem.getGpsCoords().getDecLatitude(),
                uploadItem.getGpsCoords().getDecLongitude(), 0.0f));
    }

    @Override
    public void updateMediaDetails(List<UploadMediaDetail> uploadMediaDetails) {
        uploadMediaDetailAdapter.setItems(uploadMediaDetails);
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
        btnCopyToSubsequentMedia.setAlpha(isNotEmpty ? 1.0f: 0.5f);
        btnNext.setEnabled(isNotEmpty);
        btnNext.setClickable(isNotEmpty);
        btnNext.setAlpha(isNotEmpty ? 1.0f: 0.5f);
    }

    public interface UploadMediaDetailFragmentCallback extends Callback {

        void deletePictureAtIndex(int index);
    }


    @OnClick(R.id.btn_copy_subsequent_media)
    public void onButtonCopyTitleDescToSubsequentMedia(){
        presenter.copyTitleAndDescriptionToSubsequentMedia(callback.getIndexInViewFlipper(this));
        Toast.makeText(getContext(), getResources().getString(R.string.copied_successfully), Toast.LENGTH_SHORT).show();
    }

}
