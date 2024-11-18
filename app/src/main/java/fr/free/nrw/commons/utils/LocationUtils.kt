package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import timber.log.Timber
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object LocationUtils {
    const val RADIUS_OF_EARTH_KM = 6371.0 // Earth's radius in kilometers

    @JvmStatic
    fun deriveUpdatedLocationFromSearchQuery(customQuery: String): LatLng? {
        var latLng: LatLng? = null
        val indexOfPrefix = customQuery.indexOf("Point(")
        if (indexOfPrefix == -1) {
            Timber.e("Invalid prefix index - Seems like user has entered an invalid query")
            return latLng
        }
        val indexOfSuffix = customQuery.indexOf(")\"", indexOfPrefix)
        if (indexOfSuffix == -1) {
            Timber.e("Invalid suffix index - Seems like user has entered an invalid query")
            return latLng
        }
        val latLngString = customQuery.substring(indexOfPrefix + "Point(".length, indexOfSuffix)
        if (latLngString.isEmpty()) {
            return null
        }

        val latLngArray = latLngString.split(" ")
        if (latLngArray.size != 2) {
            return null
        }

        try {
            latLng = LatLng(latLngArray[1].trim().toDouble(),
                latLngArray[0].trim().toDouble(), 1f)
        } catch (e: Exception) {
            Timber.e("Error while parsing user entered lat long: %s", e)
        }

        return latLng
    }

    @JvmStatic
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val lat1Rad = Math.toRadians(lat1)
        val lon1Rad = Math.toRadians(lon1)
        val lat2Rad = Math.toRadians(lat2)
        val lon2Rad = Math.toRadians(lon2)

        // Haversine formula
        val dlon = lon2Rad - lon1Rad
        val dlat = lat2Rad - lat1Rad
        val a = Math.pow(
                sin(dlat / 2), 2.0) + cos(lat1Rad) * cos(lat2Rad) * Math.pow(sin(dlon / 2), 2.0
            )
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return RADIUS_OF_EARTH_KM * c
    }
}
