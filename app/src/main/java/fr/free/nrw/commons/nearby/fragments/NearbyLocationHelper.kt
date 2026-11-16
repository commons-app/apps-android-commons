package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.Intent
import android.location.LocationManager
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.R
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.location.LocationPermissionsHelper.LocationPermissionCallback
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import org.osmdroid.util.GeoPoint
import timber.log.Timber

private const val DEFAULT_LATITUDE = 51.50550
private const val DEFAULT_LONGITUDE = -0.07520
private const val DEFAULT_ACCURACY = 1f
private const val LOCATION_ACCURACY_METERS = 100f
private const val DO_NOT_ASK_FOR_LOCATION_PERMISSION = "doNotAskForLocationPermission"
private const val KEY_LAST_LOCATION = "LastLocation"

/**
 * Coordinates location permissions, GPS state tracking, provider updates,
 * and location listener registration for NearbyParentFragment.
 */
class NearbyLocationHelper : NearbyParentFragmentContract.Location {
    /**
     * Interface defining callbacks for fragment-level actions triggered by location events.
     */
    interface NearbyLocationActions {
        /** Invoked when map is ready to load pins. */
        fun onMapReady()

        /** Invoked when device location changes with the specified change significance. */
        fun onUpdateMapAndList(locationChangeType: LocationChangeType)

        /** Invoked to update the user current location and error circle overlays. */
        fun onUserLocationOverlaysUpdated(
            targetP: GeoPoint,
            invalidate: Boolean,
        )

        /** Invoked to animate the map camera to a given target position. */
        fun onCameraMoveRequested(targetP: GeoPoint)

        /** Centers the map camera on the given position without animation. */
        fun onCameraCenterRequested(targetP: GeoPoint)

        /** Returns true if the user navigated from the Explore map. */
        fun isCameFromExploreMap(): Boolean

        /** Returns true if the fragment is in depicts picker mode. */
        fun isDepictsPickerMode(): Boolean

        /** Updates visibility of the progress bar during location requests. */
        fun setProgressBarVisibility(isVisible: Boolean)

        /** Displays dialog prompting the user to turn on device location. */
        fun showLocationOffDialog()

        /** Launches the fine location permission request. */
        fun askForLocationPermission()
    }

    @VisibleForTesting
    var lastKnownLocation: LatLng? = null

    @VisibleForTesting
    var isPermissionDenied: Boolean = false

    @VisibleForTesting
    var recenterToUserLocation: Boolean = false

    @VisibleForTesting
    var mapCenter: GeoPoint? = null

    private var activityProvider: (() -> Activity?)? = null
    private var locationManagerProvider: (() -> LocationServiceManager?)? = null
    private var locationPermissionsHelperProvider: (() -> LocationPermissionsHelper?)? = null
    private var applicationKvStoreProvider: (() -> JsonKvStore?)? = null
    private var actions: NearbyLocationActions? = null

    /**
     * Listener property handling location update events from LocationServiceManager.
     */
    val locationUpdateListener: LocationUpdateListener =
        object : LocationUpdateListener {
            override fun onLocationChangedSignificantly(latLng: LatLng) {
                Timber.d("Location significantly changed")
                handleLocationUpdate(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
            }

            override fun onLocationChangedSlightly(latLng: LatLng) {
                Timber.d("Location slightly changed")
                // If the map has never ever shown the current location, lets do it know
                handleLocationUpdate(latLng, LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
            }

            override fun onLocationChangedMedium(latLng: LatLng) {
                Timber.d("Location changed medium")
                // If the map has never ever shown the current location, lets do it know
                handleLocationUpdate(latLng, LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
            }
        }

    /**
     * Callback property handling location permission results.
     */
    val locationPermissionCallback: LocationPermissionCallback =
        object : LocationPermissionCallback {
            override fun onLocationPermissionDenied(toastMessage: String) = Unit

            override fun onLocationPermissionGranted() = Unit
        }

    /**
     * Initializes helper dependencies and references.
     *
     * @param activityProvider Provider for Activity
     * @param locationManagerProvider Provider for LocationServiceManager
     * @param locationPermissionsHelperProvider Provider for LocationPermissionsHelper
     * @param applicationKvStoreProvider Provider for JsonKvStore
     * @param actions Callbacks for fragment actions
     */
    fun init(
        activityProvider: () -> Activity?,
        locationManagerProvider: () -> LocationServiceManager?,
        locationPermissionsHelperProvider: () -> LocationPermissionsHelper?,
        applicationKvStoreProvider: () -> JsonKvStore?,
        actions: NearbyLocationActions,
    ) {
        this.activityProvider = activityProvider
        this.locationManagerProvider = locationManagerProvider
        this.locationPermissionsHelperProvider = locationPermissionsHelperProvider
        this.applicationKvStoreProvider = applicationKvStoreProvider
        this.actions = actions
    }

    /**
     * Requests fine location permission via the registered launcher action.
     */
    override fun askForLocationPermission() {
        Timber.d("Asking for location permission")
        actions?.askForLocationPermission()
    }

    /**
     * Verifies location permission and recenters the map to the user's location.
     *
     * @param currentLatLng Optional current location coordinate
     */
    override fun recenterMap(currentLatLng: LatLng?) {
        val activity = activityProvider?.invoke()
        val permissionsHelper = locationPermissionsHelperProvider?.invoke()
        if (activity == null || permissionsHelper == null) {
            return
        }
        val hasPermission = permissionsHelper.checkLocationPermission(activity)

        // if user has denied permission twice, then show dialog
        if (isPermissionDenied && hasPermission) {
            // this will run when user has given permission by opening app's settings
            isPermissionDenied = false
            locationPermissionGranted()
        } else {
            if (hasPermission) {
                locationPermissionGranted()
            } else {
                askForLocationPermission()
            }
            if (currentLatLng == null) {
                recenterToUserLocation = true
            }
        }
    }

    /**
     * Opens device location settings and displays a guidance toast.
     */
    override fun openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        val activity = activityProvider?.invoke() ?: return
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        val packageManager = activity.packageManager

        if (intent.resolveActivity(packageManager) != null) {
            activity.startActivity(intent)
            Toast
                .makeText(
                    activity,
                    R.string.recommend_high_accuracy_mode,
                    Toast.LENGTH_LONG,
                ).show()
        } else {
            Toast
                .makeText(
                    activity,
                    R.string.cannot_open_location_settings,
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    /**
     * Returns the last known location coordinates.
     *
     * @return LatLng of the last known location
     */
    override fun getLastLocation(): LatLng =
        lastKnownLocation ?: LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_ACCURACY)

    /**
     * Computes location where map should be centered
     *
     * @return returns the last location, if available, else returns default location
     */
    override fun getMapCenter(): LatLng {
        val kvStore = applicationKvStoreProvider?.invoke()
        val storedLocation = kvStore?.getString(KEY_LAST_LOCATION)
        if (storedLocation != null) {
            val locationLatLng = storedLocation.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            lastKnownLocation =
                LatLng(
                    locationLatLng[0].toDouble(),
                    locationLatLng[1].toDouble(),
                    DEFAULT_ACCURACY,
                )
        } else {
            lastKnownLocation =
                LatLng(
                    DEFAULT_LATITUDE,
                    DEFAULT_LONGITUDE,
                    DEFAULT_ACCURACY,
                )
        }
        val currentCenter = mapCenter
        if (currentCenter != null) {
            return LatLng(
                currentCenter.latitude,
                currentCenter.longitude,
                LOCATION_ACCURACY_METERS,
            )
        }
        return lastKnownLocation ?: LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_ACCURACY)
    }

    /**
     * Handles operations when location permission has been granted.
     */
    fun locationPermissionGranted() {
        isPermissionDenied = false
        applicationKvStoreProvider?.invoke()?.putBoolean(DO_NOT_ASK_FOR_LOCATION_PERMISSION, false)
        val manager = locationManagerProvider?.invoke()
        lastKnownLocation = manager?.getLastLocation()
        val target = lastKnownLocation

        if (target != null) {
            val targetP = GeoPoint(target.latitude, target.longitude)
            mapCenter = targetP
            actions?.onCameraCenterRequested(targetP)
            actions?.onUserLocationOverlaysUpdated(targetP, true)
            if (actions?.isCameFromExploreMap() != true && actions?.isDepictsPickerMode() != true) {
                actions?.onCameraMoveRequested(targetP)
            }
        } else if (manager != null && (manager.isGPSProviderEnabled() || manager.isNetworkProviderEnabled())) {
            manager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
            manager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
            actions?.setProgressBarVisibility(true)
        } else {
            actions?.showLocationOffDialog()
        }

        actions?.onMapReady()
        registerUnregisterLocationListener(false)
    }

    /**
     * Starts the map without GPS and without permission By default it points to DEFAULT_LATITUDE,DEFAULT_LONGITUDE
     * coordinates, other than that it points to the last known location which can be get by the key
     * "LastLocation" from applicationKvStore
     */
    fun startMapWithoutPermission() {
        val kvStore = applicationKvStoreProvider?.invoke()
        val storedLocation = kvStore?.getString(KEY_LAST_LOCATION)
        if (storedLocation != null) {
            val locationLatLng = storedLocation.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            lastKnownLocation =
                LatLng(
                    locationLatLng[0].toDouble(),
                    locationLatLng[1].toDouble(),
                    DEFAULT_ACCURACY,
                )
        } else {
            lastKnownLocation = LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE, DEFAULT_ACCURACY)
        }
        val target = lastKnownLocation
        if (target != null && actions?.isCameFromExploreMap() != true) {
            actions?.onCameraMoveRequested(GeoPoint(target.latitude, target.longitude))
        }
        actions?.onMapReady()
    }

    /**
     * Registers or unregisters the location update listener with [LocationServiceManager].
     *
     * @param removeLocationListener True to unregister and remove listener, false to register
     */
    override fun registerUnregisterLocationListener(removeLocationListener: Boolean) {
        val manager = locationManagerProvider?.invoke() ?: return
        try {
            if (removeLocationListener) {
                manager.unregisterLocationManager()
                manager.removeLocationListener(locationUpdateListener)
                Timber.d("Location service manager unregistered and removed")
            } else {
                manager.addLocationListener(locationUpdateListener)
                manager.registerLocationManager()
                Timber.d("Location service manager added and registered")
            }
        } catch (e: SecurityException) {
            Timber.e(e)
            // Broadcasts are tricky, should be catchedonR
        } catch (e: IllegalStateException) {
            Timber.e(e)
            // Broadcasts are tricky, should be catchedonR
        }
    }

    private fun handleLocationUpdate(
        latLng: LatLng,
        locationChangeType: LocationChangeType,
    ) {
        lastKnownLocation = latLng
        NearbyController.currentLocation = lastKnownLocation
        actions?.onUpdateMapAndList(locationChangeType)
        actions?.onUserLocationOverlaysUpdated(GeoPoint(latLng.latitude, latLng.longitude), true)
    }
}
