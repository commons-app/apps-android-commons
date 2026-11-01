package fr.free.nrw.commons.location

import android.location.Location

/**
 * Classifies location updates according to their freshness, accuracy, and distance.
 */
class LocationChangeEvaluator {
    companion object {
        private const val SIGNIFICANT_LOCATION_DISTANCE_IN_METERS = 1000
        private const val SIGNIFICANTLY_LESS_ACCURATE_IN_METERS = 200
        private const val MIN_LOCATION_UPDATE_TIME_DELTA_IN_MILLIS = 1000L
        private const val LOCATION_RESULTS_SIZE = 5
    }

    /**
     * Determines how much a location differs from the current best location.
     *
     * @param location the location to be tested
     * @param currentBestLocation the current best location
     * @return the type of change represented by the new location
     */
    fun evaluate(
        location: Location,
        currentBestLocation: Location?,
    ): LocationServiceManager.LocationChangeType {
        if (currentBestLocation == null) {
            return LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
        }

        val timeDelta = location.time - currentBestLocation.time
        val isSignificantlyNewer = timeDelta > MIN_LOCATION_UPDATE_TIME_DELTA_IN_MILLIS
        val isNewer = timeDelta > 0
        val accuracyDelta = (location.accuracy - currentBestLocation.accuracy).toInt()
        val isMoreAccurate = accuracyDelta < 0
        val isSignificantlyLessAccurate = accuracyDelta > SIGNIFICANTLY_LESS_ACCURATE_IN_METERS
        val isFromSameProvider = location.provider == currentBestLocation.provider

        val results = FloatArray(LOCATION_RESULTS_SIZE)
        Location.distanceBetween(
            currentBestLocation.latitude,
            currentBestLocation.longitude,
            location.latitude,
            location.longitude,
            results,
        )

        return when {
            isSignificantlyNewer ||
                isMoreAccurate ||
                (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) -> {
                if (results[0] < SIGNIFICANT_LOCATION_DISTANCE_IN_METERS) {
                    LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED
                } else {
                    LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
                }
            }
            else -> LocationServiceManager.LocationChangeType.LOCATION_NOT_CHANGED
        }
    }
}
