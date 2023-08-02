package fr.free.nrw.commons.location;

import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import fr.free.nrw.commons.R;
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
        PermissionUtils.checkPermissionsAndPerformAction(activity,
            permission.ACCESS_FINE_LOCATION,
            () -> {
                if(!isLocationAccessToAppsTurnedOn()) {
                    showLocationOffDialog(locationOffDialog);
                }
            },
            callback::onLocationPermissionDenied,
            locationAccessDialog.dialogTitleResource,
            locationAccessDialog.dialogTextResource);
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
     * Ask user to grant location access to apps
     *
     */

    private void showLocationOffDialog(Dialog locationOffDialog) {
        DialogUtil
            .showAlertDialog(activity,
                activity.getString(locationOffDialog.dialogTitleResource),
                activity.getString(locationOffDialog.dialogTextResource),
                activity.getString(R.string.title_app_shortcut_setting),
                () -> openLocationSettings(),
                true);
    }

    /**
     * Open location source settings so that apps with location access can access it
     *
     * TODO: modify it to fix https://github.com/commons-app/apps-android-commons/issues/5255
     */

    private void openLocationSettings() {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = activity.getPackageManager();

        if (intent.resolveActivity(packageManager)!= null) {
            activity.startActivity(intent);
        }
    }

    /**
     * Handle onPermissionDenied within individual classes based on the requirements
     */
    public interface LocationPermissionCallback {
        void onLocationPermissionDenied();
    }
}
