package fr.free.nrw.commons.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

class LocationServiceManager(
    private val context: Context,
) : LocationListenerAdapter() {
    companion object {
        // Maybe these values can be improved for efficiency
        private const val MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS = 10 * 100L
        private const val MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS = 1f
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    private var lastLocationVar: Location? = null
    private val locationListeners = CopyOnWriteArrayList<LocationUpdateListener>()
    private var isLocationManagerRegistered = false
    private val locationExplanationDisplayed = mutableSetOf<Activity>()
    private val locationChangeEvaluator = LocationChangeEvaluator()

    /**
     * Constructs a new instance of LocationServiceManager.
     *
     */
    fun getLastLocation(): LatLng? {
        if (lastLocationVar == null) {
            lastLocationVar = getLastKnownLocation()
            return lastLocationVar?.let { LatLng.from(it) }
        }
        return LatLng.from(lastLocationVar!!)
    }

    private fun getLastKnownLocation(): Location? {
        val providers = locationManager.getProviders(true)
        var bestLocation: Location? = null
        for (provider in providers) {
            val location: Location? =
                if (
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                    ==
                    PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    )
                    ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    locationManager.getLastKnownLocation(provider)
                } else {
                    null
                }

            if (
                location != null &&
                (bestLocation == null || location.accuracy < bestLocation.accuracy)
            ) {
                bestLocation = location
            }
        }
        return bestLocation
    }

    /**
     * Registers a LocationManager to listen for current location.
     */
    fun registerLocationManager() {
        if (!isLocationManagerRegistered) {
            isLocationManagerRegistered =
                requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER) &&
                requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER)
        }
    }

    /**
     * Requests location updates from the specified provider.
     *
     * @param locationProvider the location provider
     * @return true if successful
     */
    fun requestLocationUpdatesFromProvider(locationProvider: String): Boolean =
        try {
            if (locationManager.allProviders.contains(locationProvider)) {
                locationManager.requestLocationUpdates(
                    locationProvider,
                    MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS,
                    MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS,
                    this,
                )
                true
            } else {
                false
            }
        } catch (e: IllegalArgumentException) {
            Timber.e(e, "Illegal argument exception")
            false
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception")
            false
        }

    /**
     * Returns whether a given location is better than the current best location.
     *
     * @param location the location to be tested
     * @param currentBestLocation the current best location
     * @return LOCATION_SIGNIFICANTLY_CHANGED if location changed significantly
     * LOCATION_SLIGHTLY_CHANGED if location changed slightly
     */
    /**
     * Unregisters location manager.
     */
    fun unregisterLocationManager() {
        isLocationManagerRegistered = false
        locationExplanationDisplayed.clear()
        try {
            locationManager.removeUpdates(this)
        } catch (e: SecurityException) {
            Timber.e(e, "Security exception")
        }
    }

    /**
     * Adds a new listener to the list of location listeners.
     *
     * @param listener the new listener
     */
    fun addLocationListener(listener: LocationUpdateListener) {
        if (!locationListeners.contains(listener)) {
            locationListeners.add(listener)
        }
    }

    /**
     * Removes a listener from the list of location listeners.
     *
     * @param listener the listener to be removed
     */
    fun removeLocationListener(listener: LocationUpdateListener) {
        locationListeners.remove(listener)
    }

    override fun onLocationChanged(location: Location) {
        Timber.d("on location changed")
        val changeType = locationChangeEvaluator.evaluate(location, lastLocationVar)
        if (changeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED) {
            lastLocationVar = location
            locationListeners.forEach { it.onLocationChangedSignificantly(LatLng.from(location)) }
        } else if (lastLocationVar?.let { location.distanceTo(it) }!! >= 500) {
            lastLocationVar = location
            locationListeners.forEach { it.onLocationChangedMedium(LatLng.from(location)) }
        } else if (changeType == LocationChangeType.LOCATION_SLIGHTLY_CHANGED) {
            lastLocationVar = location
            locationListeners.forEach { it.onLocationChangedSlightly(LatLng.from(location)) }
        }
    }

    fun isNetworkProviderEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

    fun isGPSProviderEnabled(): Boolean = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

    enum class LocationChangeType {
        LOCATION_SIGNIFICANTLY_CHANGED,
        LOCATION_SLIGHTLY_CHANGED,
        LOCATION_MEDIUM_CHANGED,
        LOCATION_NOT_CHANGED,
        PERMISSION_JUST_GRANTED,
        MAP_UPDATED,
        SEARCH_CUSTOM_AREA,
        CUSTOM_QUERY,
    }
}
