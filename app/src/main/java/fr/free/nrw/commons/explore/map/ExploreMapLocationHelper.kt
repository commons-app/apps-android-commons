package fr.free.nrw.commons.explore.map

import android.Manifest
import android.app.Activity
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.utils.MapUtils
import fr.free.nrw.commons.utils.NetworkUtils.isInternetConnectionEstablished
import timber.log.Timber
import javax.inject.Inject

/**
 * Helper class that coordinates location permission requests, location updates,
 * and location listener registration for ExploreMapFragment.
 */
class ExploreMapLocationHelper
    @Inject
    constructor() :
    ExploreMapContract.Location,
        LocationUpdateListener,
        LocationPermissionCallback {
        @VisibleForTesting
        var locationManager: LocationServiceManager? = null

        @VisibleForTesting
        var locationPermissionsHelper: LocationPermissionsHelper? = null

        var activityProvider: (() -> Activity?)? = null
        var fragmentVisibilityProvider: (() -> Boolean)? = null
        var isResumedProvider: (() -> Boolean)? = null
        var onMapReadyActions: (() -> Unit)? = null
        var onDrawMyLocationMarker: (() -> Unit)? = null
        var onAskForLocationPermission: (() -> Unit)? = null
        var onLocationUpdate: ((LatLng, LocationChangeType) -> Unit)? = null
        var onPermissionGrantedCallback: (() -> Unit)? = null
        var lastKnownLocation: LatLng? = null
        var shouldPerformMapReadyActionsOnResume: Boolean = false

        /**
         * Requests location updates if appropriate permissions and visibility allow.
         */
        fun requestLocationIfNeeded() {
            if (isResumedProvider?.invoke() == true) {
                onMapReadyActions?.invoke()
            } else {
                shouldPerformMapReadyActionsOnResume = true
            }
            if (fragmentVisibilityProvider?.invoke() != true) {
                return // skips if not visible to user
            }
            val activity = activityProvider?.invoke()
            val helper = locationPermissionsHelper
            val manager = locationManager

            if (activity != null && helper != null && manager != null) {
                if (helper.checkLocationPermission(activity)) {
                    if (helper.isLocationAccessToAppsTurnedOn()) {
                        manager.registerLocationManager()
                        onDrawMyLocationMarker?.invoke()
                    } else {
                        helper.showLocationOffDialog(activity, R.string.location_off_dialog_text)
                    }
                } else {
                    helper.requestForLocationAccess(
                        R.string.location_permission_title,
                        R.string.location_permission_rationale,
                    )
                }
            }
        }

        /**
         * Checks whether active internet connection is available.
         *
         * @return True if connected, false otherwise
         */
        override fun isNetworkConnectionEstablished(): Boolean {
            val activity = activityProvider?.invoke() ?: return false
            return isInternetConnectionEstablished(activity)
        }

        /**
         * Triggers the fine location permission request launcher.
         */
        override fun askForLocationPermission() {
            Timber.d("Asking for location permission")
            onAskForLocationPermission?.invoke()
        }

        /**
         * Returns the last known location from cache or LocationServiceManager.
         *
         * @return LatLng of the last known location, or null if unavailable
         */
        override fun getLastLocation(): LatLng? {
            if (lastKnownLocation == null) {
                lastKnownLocation = locationManager?.getLastLocation()
            }
            return lastKnownLocation
        }

        /**
         * Registers or unregisters the location listener with LocationServiceManager.
         *
         * @param removeLocationListener True to unregister, false to register
         */
        fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
            val manager = locationManager ?: return
            MapUtils.registerUnregisterLocationListener(removeLocationListener, manager, this)
        }

        /**
         * Callback invoked when location permission is granted.
         */
        override fun onLocationPermissionGranted() {
            val activity = activityProvider?.invoke()
            val helper = locationPermissionsHelper
            if (activity != null && helper != null) {
                if (helper.isLocationAccessToAppsTurnedOn()) {
                    locationManager?.registerLocationManager()
                    onDrawMyLocationMarker?.invoke()
                } else {
                    helper.showLocationOffDialog(activity, R.string.location_off_dialog_text)
                }
            }
            onPermissionGrantedCallback?.invoke()
        }

        /**
         * Callback invoked when location permission is denied.
         *
         * @param toastMessage Toast message explaining denial
         */
        override fun onLocationPermissionDenied(toastMessage: String) = Unit

        /**
         * Callback invoked on significant location movement.
         *
         * @param latLng New coordinates
         */
        override fun onLocationChangedSignificantly(latLng: LatLng) {
            lastKnownLocation = latLng
            onLocationUpdate?.invoke(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        }

        /**
         * Callback invoked on slight location movement.
         *
         * @param latLng New coordinates
         */
        override fun onLocationChangedSlightly(latLng: LatLng) {
            lastKnownLocation = latLng
            onLocationUpdate?.invoke(latLng, LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
        }

        /**
         * Callback invoked on medium location movement.
         *
         * @param latLng New coordinates
         */
        override fun onLocationChangedMedium(latLng: LatLng) = Unit
    }

/**
 * Executes pending map ready actions if scheduled for resume.
 */
fun ExploreMapLocationHelper.checkAndPerformResumeMapReadyActions() {
    if (shouldPerformMapReadyActionsOnResume) {
        shouldPerformMapReadyActionsOnResume = false
        onMapReadyActions?.invoke()
    }
}

/**
 * Binds location dependencies and permission callbacks to the helper.
 *
 * @param fragment Host ExploreMapFragment instance
 */
fun ExploreMapLocationHelper.attachLocationHelper(fragment: ExploreMapFragment) {
    locationManager = fragment.locationManager
    locationPermissionsHelper = fragment.locationPermissionsHelper
    activityProvider = { fragment.activity }
    fragmentVisibilityProvider = { fragment.isVisible }
    isResumedProvider = { fragment.isResumed }
    onMapReadyActions = { fragment.performMapReadyActions() }
    onAskForLocationPermission = {
        fragment.activityResultLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
    }
    onDrawMyLocationMarker = {
        fragment.markerOverlayHelper.drawMyLocationMarker(fragment.locationManager.getLastLocation())
    }
    onLocationUpdate = { latLng, changeType ->
        fragment.exploreMapController.currentLocation = latLng
        fragment.presenter?.updateMap(changeType)
    }
    onPermissionGrantedCallback = { fragment.onLocationPermissionGrantedAction() }
}
