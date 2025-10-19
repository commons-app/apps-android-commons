package fr.free.nrw.commons.location

import android.Manifest.permission
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.Constants.RequestCodes
import fr.free.nrw.commons.utils.DialogUtil
import fr.free.nrw.commons.utils.PermissionUtils

/**
 * Helper class to handle location permissions.
 *
 * Location flow for fragments containing a map is as follows:
 * Case 1: When location permission has never been asked for or denied before
 * Check if permission is already granted or not.
 * If not already granted, ask for it (if it isn't denied twice before).
 * If now user grants permission, go to Case 3/4, else go to Case 2.
 *
 * Case 2: When location permission is just asked but has been denied
 * Shows a toast to tell the user why location permission is needed.
 * Also shows a rationale to the user, on agreeing to which, we go back to Case 1.
 * Show current location / nearby pins / nearby images according to the default location.
 *
 * Case 3: When location permission are already granted, but location services are off
 * Asks the user to turn on the location service, using a dialog.
 * If the user rejects, checks for the last known location and shows stuff using that location.
 * Also displays a toast telling the user why location should be turned on.
 *
 * Case 4: When location permission has been granted and location services are also on
 * Do whatever is required by that particular activity / fragment using current location.
 *
 */
class LocationPermissionsHelper(
    private val activity: Activity,
    private val locationManager: LocationServiceManager,
    private val callback: LocationPermissionCallback?
) {

    /**
     * Ask for location permission if the user agrees on attaching location with pictures and the
     * app does not have the access to location
     *
     * @param dialogTitleResource Resource id of the title of the dialog
     * @param dialogTextResource Resource id of the text of the dialog
     */
    fun requestForLocationAccess(
        dialogTitleResource: Int,
        dialogTextResource: Int
    ) {
        if (checkLocationPermission(activity)) {
            callback?.onLocationPermissionGranted()
        } else {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    activity,
                    permission.ACCESS_FINE_LOCATION
                )
            ) {
                DialogUtil.showAlertDialog(
                    activity,
                    activity.getString(dialogTitleResource),
                    activity.getString(dialogTextResource),
                    activity.getString(R.string.ok),
                    activity.getString(R.string.cancel),
                    {
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(permission.ACCESS_FINE_LOCATION),
                            1
                        )
                    },
                    {
                        callback?.onLocationPermissionDenied(
                            activity.getString(R.string.upload_map_location_access)
                        )
                    },
                    null
                )
            } else {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(permission.ACCESS_FINE_LOCATION),
                    RequestCodes.LOCATION
                )
            }
        }
    }

    /**
     * Shows a dialog for user to open the settings page and turn on location services
     *
     * @param activity Activity object
     * @param dialogTextResource int id of the required string resource
     */
    fun showLocationOffDialog(activity: Activity, dialogTextResource: Int) {
        DialogUtil.showAlertDialog(
            activity,
            activity.getString(R.string.ask_to_turn_location_on),
            activity.getString(dialogTextResource),
            activity.getString(R.string.title_app_shortcut_setting),
            activity.getString(R.string.cancel),
            { openLocationSettings(activity) },
            {
                Toast.makeText(
                    activity,
                    activity.getString(dialogTextResource),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    /**
     * Opens the location access page in settings, for user to turn on location services
     *
     * @param activity Activity object
     */
    fun openLocationSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val packageManager = activity.packageManager

        if (intent.resolveActivity(packageManager) != null) {
            activity.startActivity(intent)
        } else {
            Toast.makeText(activity, R.string.cannot_open_location_settings, Toast.LENGTH_LONG)
                .show()
        }
    }

    /**
     * Shows a dialog for user to open the app's settings page and give location permission
     *
     * @param activity Activity object
     * @param dialogTextResource int id of the required string resource
     */
    fun showAppSettingsDialog(activity: Activity, dialogTextResource: Int) {
        DialogUtil.showAlertDialog(
            activity,
            activity.getString(R.string.location_permission_title),
            activity.getString(dialogTextResource),
            activity.getString(R.string.title_app_shortcut_setting),
            activity.getString(R.string.cancel),
            { openAppSettings(activity) },
            {
                Toast.makeText(
                    activity,
                    activity.getString(dialogTextResource),
                    Toast.LENGTH_LONG
                ).show()
            }
        )
    }

    /**
     * Opens detailed settings page of the app for the user to turn on location services
     *
     * @param activity Activity object
     */
    private fun openAppSettings(activity: Activity) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri = Uri.fromParts("package", activity.packageName, null)
        intent.data = uri
        activity.startActivity(intent)
    }

    /**
     * Check if apps have access to location even after having individual access
     *
     * @return Returns true if location services are on and false otherwise
     */
    fun isLocationAccessToAppsTurnedOn(): Boolean {
        return locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled()
    }

    /**
     * Checks if location permission is already granted or not
     *
     * @param activity Activity object
     * @return Returns true if location permission is granted and false otherwise
     */
    fun checkLocationPermission(activity: Activity): Boolean {
        return PermissionUtils.hasPermission(
            activity,
            arrayOf(permission.ACCESS_FINE_LOCATION)
        )
    }

    /**
     * Handle onPermissionDenied within individual classes based on the requirements
     */
    interface LocationPermissionCallback {
        fun onLocationPermissionDenied(toastMessage: String)
        fun onLocationPermissionGranted()
    }
}
