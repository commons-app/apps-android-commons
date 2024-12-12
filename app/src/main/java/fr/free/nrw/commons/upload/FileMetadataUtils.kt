package fr.free.nrw.commons.upload

import androidx.exifinterface.media.ExifInterface
import timber.log.Timber

/**
 * Support utils for EXIF metadata handling
 *
 */
object FileMetadataUtils {
    /**
     * Takes EXIF label from sharedPreferences as input and returns relevant EXIF tags
     *
     * @param pref EXIF sharedPreference label
     * @return EXIF tags
     */
    fun getTagsFromPref(pref: String): Array<String> {
        Timber.d("Retuning tags for pref:%s", pref)
        return when (pref) {
            "Author" -> arrayOf(
                ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_CAMERA_OWNER_NAME
            )

            "Copyright" -> arrayOf(
                ExifInterface.TAG_COPYRIGHT
            )

            "Location" -> arrayOf(
                ExifInterface.TAG_GPS_LATITUDE,
                ExifInterface.TAG_GPS_LATITUDE_REF,
                ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_GPS_LONGITUDE_REF,
                ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_GPS_ALTITUDE_REF
            )

            "Camera Model" -> arrayOf(
                ExifInterface.TAG_MAKE,
                ExifInterface.TAG_MODEL
            )

            "Lens Model" -> arrayOf(
                ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL,
                ExifInterface.TAG_LENS_SPECIFICATION
            )

            "Serial Numbers" -> arrayOf(
                ExifInterface.TAG_BODY_SERIAL_NUMBER,
                ExifInterface.TAG_LENS_SERIAL_NUMBER
            )

            "Software" -> arrayOf(
                ExifInterface.TAG_SOFTWARE
            )

            else -> arrayOf()
        }
    }
}
