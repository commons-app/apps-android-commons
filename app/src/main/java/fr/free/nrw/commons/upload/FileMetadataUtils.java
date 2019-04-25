package fr.free.nrw.commons.upload;

import io.reactivex.Observable;
import timber.log.Timber;

import static androidx.exifinterface.media.ExifInterface.*;

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
    public static Observable<String> getTagsFromPref(String pref) {
        Timber.d("Retuning tags for pref:%s", pref);
        switch (pref) {
            case "Author":
                return Observable.fromArray(TAG_ARTIST, TAG_CAMARA_OWNER_NAME);
            case "Copyright":
                return Observable.fromArray(TAG_COPYRIGHT);
            case "Location":
                return Observable.fromArray(TAG_GPS_LATITUDE, TAG_GPS_LATITUDE_REF,
                        TAG_GPS_LONGITUDE, TAG_GPS_LONGITUDE_REF,
                        TAG_GPS_ALTITUDE, TAG_GPS_ALTITUDE_REF);
            case "Camera Model":
                return Observable.fromArray(TAG_MAKE, TAG_MODEL);
            case "Lens Model":
                return Observable.fromArray(TAG_LENS_MAKE, TAG_LENS_MODEL, TAG_LENS_SPECIFICATION);
            case "Serial Numbers":
                return Observable.fromArray(TAG_BODY_SERIAL_NUMBER, TAG_LENS_SERIAL_NUMBER);
            case "Software":
                return Observable.fromArray(TAG_SOFTWARE);
            default:
                return null;
        }
    }

}
