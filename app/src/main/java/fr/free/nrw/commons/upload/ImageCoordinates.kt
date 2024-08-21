package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.location.LatLng
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

/**
 * Extracts geolocation to be passed to API for category suggestions. If a picture with geolocation
 * is uploaded, extract latitude and longitude from EXIF data of image.
 * Otherwise, if current user location is available while using the in-app camera,
 * use it to set image coordinates
 */
class ImageCoordinates internal constructor(exif: ExifInterface?, inAppPictureLocation: LatLng?) {
    var decLatitude = 0.0
    var decLongitude = 0.0
    var imageCoordsExists = false
    /**
     * @return string of `"[decLatitude]|[decLongitude]"` or null if coordinates do not exist
     */
    var decimalCoords: String? = null
    var zoomLevel : Double = 16.0
    /**
     *  @return double value of zoom or 16.0 by default
     */

    /**
     * Construct from a stream.
     */
    internal constructor(stream: InputStream, inAppPictureLocation: LatLng?) : this(ExifInterface(stream), inAppPictureLocation)
    /**
     * Construct from the file path of the image.
     * @param path file path of the image
     */
    @Throws(IOException::class)
    internal constructor(path: String, inAppPictureLocation: LatLng?) : this(ExifInterface(path), inAppPictureLocation)

    init {
        //If image has no EXIF data and user has enabled GPS setting, get user's location
        //Always return null as a temporary fix for #1599
        if (exif != null) {
            val latAndLong = exif.latLong
            if (latAndLong != null) {
                decLatitude = latAndLong[0]
                decLongitude = latAndLong[1]
            } else {
                val latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE)
                val latitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF)
                val longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE)
                val longitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF)
                if (latitude != null && longitude != null && latitudeRef != null && longitudeRef != null) {
                    decLatitude =
                        if (ExifInterface.LATITUDE_NORTH == latitudeRef) convertToDegree(latitude)
                        else 0 - convertToDegree(latitude)
                    decLongitude =
                        if (ExifInterface.LONGITUDE_EAST == longitudeRef) convertToDegree(longitude)
                        else 0 - convertToDegree(longitude)
                }
            }
            if (!(decLatitude == 0.0 && decLongitude == 0.0)) {
                decimalCoords = "$decLatitude|$decLongitude"
                //If image has EXIF data, extract image coords
                imageCoordsExists = true
                Timber.d("EXIF data has location info")
            } else if (inAppPictureLocation != null) {
                decLatitude = inAppPictureLocation.latitude
                decLongitude = inAppPictureLocation.longitude
                if (!(decLatitude == 0.0 && decLongitude == 0.0)) {
                    decimalCoords = "$decLatitude|$decLongitude"
                    imageCoordsExists = true
                    Timber.d("Image coordinates recorded while using in-app camera")
                }
            }
        }
    }

    val latLng: LatLng? get() = LatLng(decLatitude, decLongitude, -1.0f)

    /**
     * Convert a string to an accurate Degree
     *
     * @param degreeMinuteSecondString - template string "a/b,c/d,e/f" where the letters represent numbers
     * @return the degree accurate to the second
     */
    private fun convertToDegree(degreeMinuteSecondString: String) =
        degreeMinuteSecondString.split(",").let {
            val degrees = evaluateExpression(it[0])
            val minutes = evaluateExpression(it[1])
            val seconds = evaluateExpression(it[2])
            degrees + minutes / 60 + seconds / 3600
        }

    private fun evaluateExpression(dm: String) =
        dm.split("/").let { it[0].toDouble() / it[1].toDouble() }
}
