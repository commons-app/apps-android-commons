package fr.free.nrw.commons.location;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import androidx.core.app.ActivityCompat;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.filepicker.Constants;
import fr.free.nrw.commons.filepicker.Constants.RequestCodes;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.PermissionUtils;

/**
 * Helper class to handle location permissions
 */
public class LocationPermissionsHelper {
    Activity activity;
    LocationServiceManager locationManager;
    LocationPermissionCallback callback;
    public LocationPermissionsHelper(Activity activity, LocationServiceManager locationManager,
        LocationPermissionCallback callback) {
        this.activity = activity;
        this.locationManager = locationManager;
        this.callback = callback;
    }
    public static class Dialog {
        int dialogTitleResource;
        int dialogTextResource;

        public Dialog(int dialogTitle, int dialogText) {
            dialogTitleResource = dialogTitle;
            dialogTextResource = dialogText;
        }
    }

    /**
     * Handles the entire location permissions flow
     *
     * @param locationAccessDialog
     * @param locationOffDialog
     */
    public void handleLocationPermissions(Dialog locationAccessDialog,
                                          Dialog locationOffDialog) {
        requestForLocationAccess(locationAccessDialog, locationOffDialog);
    }

    /**
     * Ask for location permission if the user agrees on attaching location with pictures
     * and the app does not have the access to location
     *
     * @param locationAccessDialog
     * @param locationOffDialog
     */
    private void requestForLocationAccess(
        Dialog locationAccessDialog,
        Dialog locationOffDialog
    ) {
        if (PermissionUtils.hasPermission(activity, new String[]{permission.ACCESS_FINE_LOCATION})) {
            callback.onLocationPermissionGranted();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission.ACCESS_FINE_LOCATION)) {
                if (locationAccessDialog != null && locationOffDialog != null) {
                    DialogUtil.showAlertDialog(activity, activity.getString(locationAccessDialog.dialogTitleResource),
                        activity.getString(locationAccessDialog.dialogTextResource),
                        activity.getString(android.R.string.ok),
                        activity.getString(android.R.string.cancel),
                        () -> {
                            if (!isLocationAccessToAppsTurnedOn()) {
                                showLocationOffDialog(activity);
                            } else {
                                ActivityCompat.requestPermissions(activity,
                                    new String[]{permission.ACCESS_FINE_LOCATION}, 1);
                            }
                        },
                        () -> callback.onLocationPermissionDenied(activity.getString(R.string.in_app_camera_location_permission_denied)),
                        null,
                        false);
                }
            } else {
                ActivityCompat.requestPermissions(activity, new String[]{permission.ACCESS_FINE_LOCATION},
                    RequestCodes.LOCATION);
            }
        }
    }

    public void showLocationOffDialog(Activity activity) {
        DialogUtil
            .showAlertDialog(activity,
                activity.getString(R.string.ask_to_turn_location_on),
                activity.getString(R.string.in_app_camera_needs_location),
                activity.getString(R.string.title_app_shortcut_setting),
                activity.getString(R.string.cancel),
                () -> openLocationSettings(activity),
                () -> callback.onLocationPermissionDenied(activity.getString(
                    R.string.in_app_camera_location_unavailable)));
    }

    /**
     * Open location source settings so that apps with location access can access it
     *
     * TODO: modify it to fix https://github.com/commons-app/apps-android-commons/issues/5255
     */

    public void openLocationSettings(Activity activity) {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = activity.getPackageManager();

        if (intent.resolveActivity(packageManager)!= null) {
            activity.startActivity(intent);
        }
    }


    /**
     * Check if apps have access to location even after having individual access
     *
     * @return
     */
    public boolean isLocationAccessToAppsTurnedOn() {
        return (locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled());
    }

    /**
     * Handle onPermissionDenied within individual classes based on the requirements
     */
    public interface LocationPermissionCallback {
        void onLocationPermissionDenied(String toastMessage);
        void onLocationPermissionGranted();
    }
}
