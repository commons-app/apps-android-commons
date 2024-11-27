package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.location.LocationUpdateListener
import timber.log.Timber

object MapUtils {
    const val ZOOM_LEVEL = 14f
    const val CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005
    const val CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.004
    const val NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE"
    const val ZOOM_OUT = 0f

    @JvmStatic
    val defaultLatLng = LatLng(51.50550, -0.07520, 1f)

    @JvmStatic
    fun registerUnregisterLocationListener(
        removeLocationListener: Boolean,
        locationManager: LocationServiceManager,
        locationUpdateListener: LocationUpdateListener
    ) {
        try {
            if (removeLocationListener) {
                locationManager.unregisterLocationManager()
                locationManager.removeLocationListener(locationUpdateListener)
                Timber.d("Location service manager unregistered and removed")
            } else {
                locationManager.addLocationListener(locationUpdateListener)
                locationManager.registerLocationManager()
                Timber.d("Location service manager added and registered")
            }
        } catch (e: Exception) {
            Timber.e(e)
            // Broadcasts are tricky, should be caught on onR
        }
    }
}
