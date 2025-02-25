package fr.free.nrw.commons.location

import android.location.Location
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round


/**
 * A latitude and longitude point with accuracy information, often of a picture.
 */
data class LatLng(
    var latitude: Double,
    var longitude: Double,
    val accuracy: Float
) : Parcelable {

    /**
     * Accepts latitude and longitude.
     * North and South values are cut off at 90°
     *
     * Examples:
     * the Statue of Liberty is located at 40.69° N, 74.04° W
     * The Statue of Liberty could be constructed as LatLng(40.69, -74.04, 1.0)
     * where positive signifies north, east and negative signifies south, west.
     */
    init {
        val adjustedLongitude = when {
            longitude in -180.0..180.0 -> longitude
            else -> ((longitude - 180.0) % 360.0 + 360.0) % 360.0 - 180.0
        }
        latitude = max(-90.0, min(90.0, latitude))
        longitude = adjustedLongitude
    }

    /**
     * Accepts a non-null [Location] and converts it to a [LatLng].
     */
    companion object {
        fun latLongOrNull(latitude: String?, longitude: String?): LatLng? =
            if (!latitude.isNullOrBlank() && !longitude.isNullOrBlank()) {
                LatLng(latitude.toDouble(), longitude.toDouble(), 0.0f)
            } else {
                null
            }

        /**
         * gets the latitude and longitude of a given non-null location
         * @param location the non-null location of the user
         * @return LatLng the Latitude and Longitude of a given location
         */
        @JvmStatic
        fun from(location: Location): LatLng {
            return LatLng(location.latitude, location.longitude, location.accuracy)
        }

        @JvmField
        val CREATOR: Parcelable.Creator<LatLng> = object : Parcelable.Creator<LatLng> {
            override fun createFromParcel(parcel: Parcel): LatLng {
                return LatLng(parcel)
            }

            override fun newArray(size: Int): Array<LatLng?> {
                return arrayOfNulls(size)
            }
        }
    }

    /**
     * An alternate constructor for this class.
     * @param parcel A parcelable which contains the latitude, longitude, and accuracy
     */
    private constructor(parcel: Parcel) : this(
        latitude = parcel.readDouble(),
        longitude = parcel.readDouble(),
        accuracy = parcel.readFloat()
    )

    /**
     * Creates a hash code for the latitude and longitude.
     */
    override fun hashCode(): Int {
        var result = 1
        val latitudeBits = latitude.toBits()
        result = 31 * result + (latitudeBits xor (latitudeBits ushr 32)).toInt()
        val longitudeBits = longitude.toBits()
        result = 31 * result + (longitudeBits xor (longitudeBits ushr 32)).toInt()
        return result
    }

    /**
     * Checks for equality of two LatLng objects.
     * @param other the second LatLng object
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LatLng) return false
        return latitude.toBits() == other.latitude.toBits() &&
                longitude.toBits() == other.longitude.toBits()
    }

    /**
     * Returns a string representation of the latitude and longitude.
     */
    override fun toString(): String {
        return "lat/lng: ($latitude,$longitude)"
    }

    /**
     * Returns a nicely formatted coordinate string. Used e.g. in
     * the detail view.
     *
     * @return The formatted string.
     */
    fun getPrettyCoordinateString(): String {
        return "${formatCoordinate(latitude)} ${getNorthSouth()}, " +
                "${formatCoordinate(longitude)} ${getEastWest()}"
    }

    /**
     * Gets a URI for a Google Maps intent at the location.
     *
     * @paraam zoom The zoom level
     * @return URI for the intent
     */
    fun getGmmIntentUri(zoom: Double): Uri = Uri.parse(
        "geo:$latitude,$longitude?q=$latitude,$longitude&z=${zoom}"
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeFloat(accuracy)
    }

    override fun describeContents(): Int = 0

    private fun formatCoordinate(coordinate: Double): String {
        val roundedNumber = round(coordinate * 10000) / 10000
        return abs(roundedNumber).toString()
    }

    /**
     * Returns "N" or "S" depending on the latitude.
     *
     * @return "N" or "S".
     */
    private fun getNorthSouth(): String = if (latitude < 0) "S" else "N"

    /**
     * Returns "E" or "W" depending on the longitude.
     *
     * @return "E" or "W".
     */
    private fun getEastWest(): String = if (longitude in 0.0..179.999) "E" else "W"
}