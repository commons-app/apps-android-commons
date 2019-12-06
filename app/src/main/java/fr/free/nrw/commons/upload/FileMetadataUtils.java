package fr.free.nrw.commons.upload;

import timber.log.Timber;

import static androidx.exifinterface.media.ExifInterface.TAG_ARTIST;
import static androidx.exifinterface.media.ExifInterface.TAG_BODY_SERIAL_NUMBER;
import static androidx.exifinterface.media.ExifInterface.TAG_CAMARA_OWNER_NAME;
import static androidx.exifinterface.media.ExifInterface.TAG_COPYRIGHT;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_ALTITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LATITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE;
import static androidx.exifinterface.media.ExifInterface.TAG_GPS_LONGITUDE_REF;
import static androidx.exifinterface.media.ExifInterface.TAG_LENS_MAKE;
import static androidx.exifinterface.media.ExifInterface.TAG_LENS_MODEL;
import static androidx.exifinterface.media.ExifInterface.TAG_LENS_SERIAL_NUMBER;
import static androidx.exifinterface.media.ExifInterface.TAG_LENS_SPECIFICATION;
import static androidx.exifinterface.media.ExifInterface.TAG_MAKE;
import static androidx.exifinterface.media.ExifInterface.TAG_MODEL;
import static androidx.exifinterface.media.ExifInterface.TAG_SOFTWARE;

/**
 * Support utils for EXIF metadata handling
 *
 */
public class FileMetadataUtils {

    /**
     * Takes EXIF label from sharedPreferences as input and returns relevant EXIF tags
     *
     * @param pref EXIF sharedPreference label
     * @return EXIF tags
     */
    public static String[] getTagsFromPref(String pref) {
        Timber.d("Retuning tags for pref:%s", pref);
        switch (pref) {
            case "Author":
                return new String[]{TAG_ARTIST, TAG_CAMARA_OWNER_NAME};
            case "Copyright":
                return new String[]{TAG_COPYRIGHT};
            case "Location":
                return new String[]{TAG_GPS_LATITUDE, TAG_GPS_LATITUDE_REF,
                        TAG_GPS_LONGITUDE, TAG_GPS_LONGITUDE_REF,
                        TAG_GPS_ALTITUDE, TAG_GPS_ALTITUDE_REF};
            case "Camera Model":
                return new String[]{TAG_MAKE, TAG_MODEL};
            case "Lens Model":
                return new String[]{TAG_LENS_MAKE, TAG_LENS_MODEL, TAG_LENS_SPECIFICATION};
            case "Serial Numbers":
                return new String[]{TAG_BODY_SERIAL_NUMBER, TAG_LENS_SERIAL_NUMBER};
            case "Software":
                return new String[]{TAG_SOFTWARE};
            default:
                return new String[]{};
        }
    }

}
