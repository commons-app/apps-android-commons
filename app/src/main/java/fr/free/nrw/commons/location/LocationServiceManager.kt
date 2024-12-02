package fr.free.nrw.commons.location

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.core.app.ActivityCompat
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList


class LocationServiceManager(private val context: Context) : LocationListener {

    companion object {
        // Maybe these values can be improved for efficiency
        private const val MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS = 10 * 100L
        private const val MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS = 1f
    }

    private val locationManager: LocationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    var lastLocationVar: Location? = null
    private val locationListeners = CopyOnWriteArrayList<LocationUpdateListener>()
    private var isLocationManagerRegistered = false
    private val locationExplanationDisplayed = mutableSetOf<Activity>()

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
            val location: Location? = if (
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION)
                ==
                PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION)
                ==
                PackageManager.PERMISSION_GRANTED
            ) {
                locationManager.getLastKnownLocation(provider)
            } else {
                null
            }

            if (
                location != null
                &&
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
    fun requestLocationUpdatesFromProvider(locationProvider: String): Boolean {
        return try {
            if (locationManager.allProviders.contains(locationProvider)) {
                locationManager.requestLocationUpdates(
                    locationProvider,
                    MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS,
                    MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS,
                    this
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
    }

    /**
     * Returns whether a given location is better than the current best location.
     *
     * @param location the location to be tested
     * @param currentBestLocation the current best location
     * @return LOCATION_SIGNIFICANTLY_CHANGED if location changed significantly
     * LOCATION_SLIGHTLY_CHANGED if location changed slightly
     */
    private fun isBetterLocation(location: Location, currentBestLocation: Location?): LocationChangeType {
        if (currentBestLocation == null) {
            return LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
        }

        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS
        val isNewer = timeDelta > 0
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > 200
        val isFromSameProvider = isSameProvider(location.provider, currentBestLocation.provider)

        val results = FloatArray(5)
        Location.distanceBetween(
            currentBestLocation.latitude, currentBestLocation.longitude,
            location.latitude, location.longitude,
            results
        )

        return when {
            isSignificantlyNewer
                    ||
                    isMoreAccurate
                    ||
                    (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) -> {
                if (results[0] < 1000) LocationChangeType.LOCATION_SLIGHTLY_CHANGED
                else LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
            }
            else -> LocationChangeType.LOCATION_NOT_CHANGED
        }
    }

    /**
     * Checks whether two providers are the same
     */
    private fun isSameProvider(provider1: String?, provider2: String?): Boolean {
        return provider1 == provider2
    }

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
        val changeType = isBetterLocation(location, lastLocationVar)
        if (changeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED) {
            lastLocationVar = location
            locationListeners.forEach { it.onLocationChangedSignificantly(LatLng.from(location)) }
        } else if (lastLocationVar?.let { location.distanceTo(it) }!! >= 500) {
            locationListeners.forEach { it.onLocationChangedMedium(LatLng.from(location)) }
        } else if (changeType == LocationChangeType.LOCATION_SLIGHTLY_CHANGED) {
            lastLocationVar = location
            locationListeners.forEach { it.onLocationChangedSlightly(LatLng.from(location)) }
        }
    }

    @Deprecated("Deprecated in Java", ReplaceWith(
        "Timber.d(\"%s's status changed to %d\", provider, status)",
        "timber.log.Timber"
    )
    )
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        Timber.d("%s's status changed to %d", provider, status)
    }



    override fun onProviderEnabled(provider: String) {
        Timber.d("Provider %s enabled", provider)
    }

    override fun onProviderDisabled(provider: String) {
        Timber.d("Provider %s disabled", provider)
    }

    fun isNetworkProviderEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun isGPSProviderEnabled(): Boolean {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    enum class LocationChangeType {
        LOCATION_SIGNIFICANTLY_CHANGED,
        LOCATION_SLIGHTLY_CHANGED,
        LOCATION_MEDIUM_CHANGED,
        LOCATION_NOT_CHANGED,
        PERMISSION_JUST_GRANTED,
        MAP_UPDATED,
        SEARCH_CUSTOM_AREA,
        CUSTOM_QUERY
    }
}









