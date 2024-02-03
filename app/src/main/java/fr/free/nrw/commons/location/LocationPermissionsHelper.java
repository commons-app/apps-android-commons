package fr.free.nrw.commons.location;

import android.Manifest;
import android.Manifest.permission;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import fr.free.nrw.commons.R;
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

    /**
     * Ask for location permission if the user agrees on attaching location with pictures and the
     * app does not have the access to location
     *
     * @param dialogTitleResource
     * @param dialogTextResource
     */
    public void requestForLocationAccess(
        int dialogTitleResource,
        int dialogTextResource
    ) {
        if (checkLocationPermission(activity)) {
            callback.onLocationPermissionGranted();
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity,
                permission.ACCESS_FINE_LOCATION)) {
                DialogUtil.showAlertDialog(activity, activity.getString(dialogTitleResource),
                    activity.getString(dialogTextResource),
                    activity.getString(android.R.string.ok),
                    activity.getString(android.R.string.cancel),
                    () -> {
                        ActivityCompat.requestPermissions(activity,
                            new String[]{permission.ACCESS_FINE_LOCATION}, 1);
                    },
                    () -> callback.onLocationPermissionDenied(
                        activity.getString(R.string.upload_map_location_access)),
                    null,
                    false);
            } else {
                ActivityCompat.requestPermissions(activity,
                    new String[]{permission.ACCESS_FINE_LOCATION},
                    RequestCodes.LOCATION);
            }
        }
    }

    /**
     * Shows a dialog for user to open the settings page and turn on location services
     *
     * @param activity Activity object
     * @param dialogTextResource int id of the required string resource
     */
    public void showLocationOffDialog(Activity activity, int dialogTextResource) {
        DialogUtil
            .showAlertDialog(activity,
                activity.getString(R.string.ask_to_turn_location_on),
                activity.getString(dialogTextResource),
                activity.getString(R.string.title_app_shortcut_setting),
                activity.getString(R.string.cancel),
                () -> openLocationSettings(activity),
                () -> Toast.makeText(activity, activity.getString(dialogTextResource),
                    Toast.LENGTH_LONG).show()
            );
    }

    /**
     * Opens the location access page in settings, for user to turn on location services
     *
     * @param activity Activtiy object
     */
    public void openLocationSettings(Activity activity) {
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = activity.getPackageManager();

        if (intent.resolveActivity(packageManager) != null) {
            activity.startActivity(intent);
        } else {
            Toast.makeText(activity, R.string.cannot_open_location_settings, Toast.LENGTH_LONG)
                .show();
        }
    }

    /**
     * Shows a dialog for user to open the app's settings page and give location permission
     *
     * @param activity Activity object
     * @param dialogTextResource int id of the required string resource
     */
    public void showAppSettingsDialog(Activity activity, int dialogTextResource) {
        DialogUtil
            .showAlertDialog(activity, activity.getString(R.string.location_permission_title),
                activity.getString(dialogTextResource),
                activity.getString(R.string.title_app_shortcut_setting),
                activity.getString(R.string.cancel),
                () -> openAppSettings(activity),
                () -> Toast.makeText(activity, activity.getString(dialogTextResource),
                    Toast.LENGTH_LONG).show()
            );
    }

    /**
     * Opens detailed settings page of the app for the user to turn on location services
     *
     * @param activity Activity object
     */
    public void openAppSettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", activity.getPackageName(), null);
        intent.setData(uri);
        activity.startActivity(intent);
    }


    /**
     * Check if apps have access to location even after having individual access
     *
     * @return Returns true ir false depending on if location services are on or not
     */
    public boolean isLocationAccessToAppsTurnedOn() {
        return (locationManager.isNetworkProviderEnabled()
            || locationManager.isGPSProviderEnabled());
    }

    /**
     * Checks if location permission is already granted or not
     *
     * @param activity Activity object
     * @return Returns true or false depending on whether location permission is granted or not
     */
    public boolean checkLocationPermission(Activity activity) {
        return PermissionUtils.hasPermission(activity,
            new String[]{Manifest.permission.ACCESS_FINE_LOCATION});
    }

    /**
     * Handle onPermissionDenied within individual classes based on the requirements
     */
    public interface LocationPermissionCallback {

        void onLocationPermissionDenied(String toastMessage);

        void onLocationPermissionGranted();
    }
}
