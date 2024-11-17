package fr.free.nrw.commons.utils

import java.text.NumberFormat
import fr.free.nrw.commons.location.LatLng
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

object LengthUtils {
    /**
     * Returns a formatted distance string between two points.
     *
     * @param point1 LatLng type point1
     * @param point2 LatLng type point2
     * @return string distance
     */
    @JvmStatic
    fun formatDistanceBetween(point1: LatLng?, point2: LatLng?): String? {
        if (point1 == null || point2 == null) {
            return null
        }

        val distance = computeDistanceBetween(point1, point2).roundToInt()
        return formatDistance(distance)
    }

    /**
     * Format a distance (in meters) as a string
     * Example: 140 -> "140m"
     * 3841 -> "3.8km"
     *
     * @param distance Distance, in meters
     * @return A string representing the distance
     * @throws IllegalArgumentException If distance is negative
     */
    @JvmStatic
    fun formatDistance(distance: Int): String {
        if (distance < 0) {
            throw IllegalArgumentException("Distance must be non-negative")
        }

        val numberFormat = NumberFormat.getNumberInstance()

        // Adjust to km if distance is over 1000m (1km)
        return if (distance >= 1000) {
            numberFormat.maximumFractionDigits = 1
            "${numberFormat.format(distance / 1000.0)}km"
        } else {
            "${numberFormat.format(distance)}m"
        }
    }

    /**
     * Computes the distance between two points.
     *
     * @param point1 LatLng type point1
     * @param point2 LatLng type point2
     * @return distance between the points in meters
     * @throws NullPointerException if one or both the points are null
     */
    @JvmStatic
    fun computeDistanceBetween(point1: LatLng, point2: LatLng): Double {
        return computeAngleBetween(point1, point2) * 6371009.0 // Earth's radius in meters
    }

    /**
     * Computes angle between two points
     *
     * @param point1 one of the two end points
     * @param point2 one of the two end points
     * @return Angle in radians
     * @throws NullPointerException if one or both the points are null
     */
    @JvmStatic
    private fun computeAngleBetween(point1: LatLng, point2: LatLng): Double {
        return distanceRadians(
            Math.toRadians(point1.latitude),
            Math.toRadians(point1.longitude),
            Math.toRadians(point2.latitude),
            Math.toRadians(point2.longitude)
        )
    }

    /**
     * Computes arc length between 2 points
     *
     * @param lat1 Latitude of point A
     * @param lng1 Longitude of point A
     * @param lat2 Latitude of point B
     * @param lng2 Longitude of point B
     * @return Arc length between the points
     */
    @JvmStatic
    private fun distanceRadians(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        return arcHav(havDistance(lat1, lat2, lng1 - lng2))
    }

    /**
     * Computes inverse of haversine
     *
     * @param x Angle in radian
     * @return Inverse of haversine
     */
    @JvmStatic
    private fun arcHav(x: Double): Double {
        return 2.0 * asin(sqrt(x))
    }

    /**
     * Computes distance between two points that are on same Longitude
     *
     * @param lat1      Latitude of point A
     * @param lat2      Latitude of point B
     * @param longitude Longitude on which they lie
     * @return Arc length between points
     */
    @JvmStatic
    private fun havDistance(lat1: Double, lat2: Double, longitude: Double): Double {
        return hav(lat1 - lat2) + hav(longitude) * cos(lat1) * cos(lat2)
    }

    /**
     * Computes haversine
     *
     * @param x Angle in radians
     * @return Haversine of x
     */
    @JvmStatic
    private fun hav(x: Double): Double {
        val sinHalf = sin(x * 0.5)
        return sinHalf * sinHalf
    }

    /**
     * Computes bearing between the two given points
     *
     * @see <a href="https://www.movable-type.co.uk/scripts/latlong.html">Bearing</a>
     * @param point1 Coordinates of first point
     * @param point2 Coordinates of second point
     * @return Bearing between the two end points in degrees
     * @throws NullPointerException if one or both the points are null
     */
    @JvmStatic
    fun computeBearing(point1: LatLng, point2: LatLng): Double {
        val diffLongitude = Math.toRadians(point2.longitude - point1.longitude)
        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val y = sin(diffLongitude) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(diffLongitude)
        val bearing = atan2(y, x)
        return (Math.toDegrees(bearing) + 360) % 360
    }
}