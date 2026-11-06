package fr.free.nrw.commons.locationpicker

import android.Manifest.permission
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.filepicker.Constants
import fr.free.nrw.commons.kvstore.BasicKvStore
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager

/**
 * Handles location permission results for [LocationPickerActivity].
 */
class LocationPickerPermissionManager(
    private val activity: LocationPickerActivity,
    private val locationManager: LocationServiceManager,
    private val store: BasicKvStore,
    private val mapManager: LocationPickerMapManager,
    private val shouldMoveToCurrentLocation: () -> Boolean,
    private val pickerSource: () -> String?,
) : LocationPermissionCallback {
    private lateinit var locationPermissionsHelper: LocationPermissionsHelper

    /**
     * Requests access to the device location.
     */
    fun requestLocationPermissions() {
        locationPermissionsHelper =
            LocationPermissionsHelper(activity, locationManager, this)
        locationPermissionsHelper.requestForLocationAccess(
            R.string.location_permission_title,
            R.string.upload_map_location_access,
        )
    }

    /**
     * Handles the result returned by the Android permission request.
     */
    fun onRequestPermissionsResult(
        requestCode: Int,
        grantResults: IntArray,
    ) {
        if (requestCode == Constants.RequestCodes.LOCATION &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            onLocationPermissionGranted()
        } else {
            onLocationPermissionDenied(activity.getString(R.string.upload_map_location_access))
        }
    }

    /**
     * Shows the appropriate explanation or settings prompt after permission denial.
     */
    override fun onLocationPermissionDenied(toastMessage: String) {
        val isDeniedBefore = store.getBoolean(IS_PERMISSION_DENIED, false)
        val showRationale =
            ActivityCompat.shouldShowRequestPermissionRationale(
                activity,
                permission.ACCESS_FINE_LOCATION,
            )

        if (!showRationale) {
            if (!locationPermissionsHelper.checkLocationPermission(activity)) {
                if (isDeniedBefore) {
                    locationPermissionsHelper.showAppSettingsDialog(
                        activity,
                        R.string.upload_map_location_access,
                    )
                } else {
                    Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
                }
                store.putBoolean(IS_PERMISSION_DENIED, true)
            }
        } else {
            Toast.makeText(activity, toastMessage, Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Starts location updates and places the current-location marker after permission is granted.
     */
    override fun onLocationPermissionGranted() {
        if (shouldMoveToCurrentLocation() || pickerSource() != MEDIA_ACTIVITY) {
            if (locationPermissionsHelper.isLocationAccessToAppsTurnedOn()) {
                locationManager.requestLocationUpdatesFromProvider(
                    android.location.LocationManager.NETWORK_PROVIDER,
                )
                locationManager.requestLocationUpdatesFromProvider(
                    android.location.LocationManager.GPS_PROVIDER,
                )
                mapManager.addMarkerAtGPSLocation(shouldMoveToCurrentLocation())
            } else {
                mapManager.addMarkerAtGPSLocation(shouldMoveToCurrentLocation())
                locationPermissionsHelper.showLocationOffDialog(
                    activity,
                    R.string.ask_to_turn_location_on_text,
                )
            }
        }
    }

    private companion object {
        const val IS_PERMISSION_DENIED = "isPermissionDenied"
        const val MEDIA_ACTIVITY = "MediaActivity"
    }
}
