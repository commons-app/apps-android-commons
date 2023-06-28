package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.annotation.NonNull;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.DefaultCallback;
import fr.free.nrw.commons.filepicker.FilePicker;
import fr.free.nrw.commons.filepicker.FilePicker.ImageSource;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.upload.UploadActivity;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class ContributionController {

    public static final String ACTION_INTERNAL_UPLOADS = "internalImageUploads";
    private final JsonKvStore defaultKvStore;
    private LatLng locationBeforeImageCapture;

    @Inject
    LocationServiceManager locationManager;
    @Inject
    public ContributionController(@Named("default_preferences") JsonKvStore defaultKvStore) {
        this.defaultKvStore = defaultKvStore;
    }

    /**
     * Check for permissions and initiate camera click
     */
    public void initiateCameraPick(Activity activity) {
        boolean useExtStorage = defaultKvStore.getBoolean("useExternalStorage", true);
        if (!useExtStorage) {
            initiateCameraUpload(activity);
            return;
        }

        PermissionUtils.checkPermissionsAndPerformAction(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                () -> {
                    if (!(PermissionUtils.hasPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)
                         && isLocationAccessToAppsTurnedOn())) {
                        askUserToAllowLocationAccess(activity);
                    } else {
                        initiateCameraUpload(activity);
                    }
                },
                R.string.storage_permission_title,
                R.string.write_storage_permission_rationale);
    }

    /**
     * Suggest user to attach location information with pictures.
     * If the user selects "Yes", then:
     *
     * Location is taken from the EXIF if the default camera application
     * does not redact location tags.
     *
     * Otherwise, if the EXIF metadata does not have location information,
     * then location captured by the app is used
     *
     * @param activity
     */
    private void askUserToAllowLocationAccess(Activity activity) {
        DialogUtil.showAlertDialog(activity,
            activity.getString(R.string.location_permission_title),
            activity.getString(R.string.in_app_camera_location_access_explanation),
            activity.getString(R.string.option_allow),
            activity.getString(R.string.option_dismiss),
            ()-> requestForLocationAccess(activity),
            () -> initiateCameraUpload(activity),
            null,
            true);
    }

    /**
     * Ask for location permission if the user agrees on attaching location with pictures
     * and the app does not have the access to location
     *
     * @param activity
     */

    private void requestForLocationAccess(Activity activity) {
        PermissionUtils.checkPermissionsAndPerformAction(activity,
            permission.ACCESS_FINE_LOCATION,
            () -> onLocationPermissionGranted(activity),
            () -> {},
            R.string.ask_to_turn_location_on,
            R.string.in_app_camera_location_permission_rationale);
    }

    /**
     * Check if apps have access to location even after having individual access
     *
     * @return
     */
    private boolean isLocationAccessToAppsTurnedOn() {
        return (locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled());
    }

    /**
     * Initiate in-app camera if apps have access to location.
     * Otherwise, show location-off dialog.
     *
     * @param activity
     */
    private void onLocationPermissionGranted(Activity activity) {
        if (!isLocationAccessToAppsTurnedOn()) {
            showLocationOffDialog(activity);
        } else {
            initiateCameraUpload(activity);
        }
    }

    /**
     * Ask user to grant location access to apps
     *
     * @param activity
     */

    private void showLocationOffDialog(Activity activity) {
        DialogUtil
            .showAlertDialog(activity,
                activity.getString(R.string.location_permission_title),
                activity.getString(R.string.in_app_camera_needs_location),
                activity.getString(R.string.title_app_shortcut_setting),
                () -> openLocationSettings(activity),
                true);
    }

    /**
     * Open location source settings so that apps with location access can access it
     *
     * @param activity
     */

    private void openLocationSettings(Activity activity) {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = activity.getPackageManager();

        if (intent.resolveActivity(packageManager)!= null) {
            activity.startActivity(intent);
        }
    }

    /**
     * Initiate gallery picker
     */
    public void initiateGalleryPick(final Activity activity, final boolean allowMultipleUploads) {
        initiateGalleryUpload(activity, allowMultipleUploads);
    }

    /**
     * Initiate gallery picker with permission
     */
    public void initiateCustomGalleryPickWithPermission(final Activity activity) {
        setPickerConfiguration(activity,true);

        PermissionUtils.checkPermissionsAndPerformAction(activity,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            () -> {
                FilePicker.openCustomSelector(activity, 0);
            },
            R.string.storage_permission_title,
            R.string.write_storage_permission_rationale);
    }


    /**
     * Open chooser for gallery uploads
     */
    private void initiateGalleryUpload(final Activity activity, final boolean allowMultipleUploads) {
        setPickerConfiguration(activity, allowMultipleUploads);
        boolean isGetContentPickerPreferred = defaultKvStore.getBoolean("getContentPhotoPickerPref");
        FilePicker.openGallery(activity, 0, isGetContentPickerPreferred);
    }

    /**
     * Sets configuration for file picker
     */
    private void setPickerConfiguration(Activity activity,
                                        boolean allowMultipleUploads) {
        boolean copyToExternalStorage = defaultKvStore.getBoolean("useExternalStorage", true);
        FilePicker.configuration(activity)
                .setCopyTakenPhotosToPublicGalleryAppFolder(copyToExternalStorage)
                .setAllowMultiplePickInGallery(allowMultipleUploads);
    }

    /**
     * Initiate camera upload by opening camera
     */
    private void initiateCameraUpload(Activity activity) {
        setPickerConfiguration(activity, false);
        locationBeforeImageCapture = locationManager.getLastLocation();
        FilePicker.openCameraForImage(activity, 0);
    }

    /**
     * Attaches callback for file picker.
     */
    public void handleActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
        FilePicker.handleActivityResult(requestCode, resultCode, data, activity, new DefaultCallback() {

            @Override
            public void onCanceled(final ImageSource source, final int type) {
                super.onCanceled(source, type);
                defaultKvStore.remove(PLACE_OBJECT);
            }

            @Override
            public void onImagePickerError(Exception e, FilePicker.ImageSource source, int type) {
                ViewUtil.showShortToast(activity, R.string.error_occurred_in_picking_images);
            }

            @Override
            public void onImagesPicked(@NonNull List<UploadableFile> imagesFiles, FilePicker.ImageSource source, int type) {
                Intent intent = handleImagesPicked(activity, imagesFiles);
                activity.startActivity(intent);
            }
        });
    }

    public List<UploadableFile> handleExternalImagesPicked(Activity activity,
                                                           Intent data) {
        return FilePicker.handleExternalImagesPicked(data, activity);
    }

    /**
     * Returns intent to be passed to upload activity
     * Attaches place object for nearby uploads and
     * location before image capture if in-app camera is used
     */
    private Intent handleImagesPicked(Context context,
        List<UploadableFile> imagesFiles) {
        Intent shareIntent = new Intent(context, UploadActivity.class);
        shareIntent.setAction(ACTION_INTERNAL_UPLOADS);
        shareIntent
            .putParcelableArrayListExtra(UploadActivity.EXTRA_FILES, new ArrayList<>(imagesFiles));
        Place place = defaultKvStore.getJson(PLACE_OBJECT, Place.class);

        if (place != null) {
            shareIntent.putExtra(PLACE_OBJECT, place);
        }

        if (locationBeforeImageCapture != null) {
            shareIntent.putExtra(
                UploadActivity.LOCATION_BEFORE_IMAGE_CAPTURE,
                locationBeforeImageCapture);
        }

        return shareIntent;
    }

}
