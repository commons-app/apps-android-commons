package fr.free.nrw.commons.upload;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import java.io.IOException;
import java.io.InputStream;
import timber.log.Timber;

/**
 * Extracts geolocation to be passed to API for category suggestions. If a picture with geolocation
 * is uploaded, extract latitude and longitude from EXIF data of image.
 */
public class ImageCoordinates {

    private double decLatitude;
    private double decLongitude;
    public boolean imageCoordsExists;
    private String decimalCoords;

    /**
     * Construct from the file path of the image.
     * @param exif exif interface of the image
     *
     */
    ImageCoordinates(ExifInterface exif){
        //If image has no EXIF data and user has enabled GPS setting, get user's location
        //Always return null as a temporary fix for #1599
        if (exif != null && exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) != null) {
            //If image has EXIF data, extract image coords
            imageCoordsExists = true;
            Timber.d("EXIF data has location info");

            String latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String longitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);
            decLatitude = "N".equals(latitudeRef) ? convertToDegree(latitude) :
                0 - convertToDegree(latitude);
            decLongitude = "E".equals(longitudeRef) ? convertToDegree(longitude)
                : 0 - convertToDegree(longitude);

            decimalCoords = decLatitude + "|" + decLongitude;
        }
    }

    /**
     * Construct from a stream.
     */
    ImageCoordinates(@NonNull InputStream stream) throws IOException {
        this(new ExifInterface(stream));
    }

    /**
     * Construct from the file path of the image.
     * @param path file path of the image
     *
     */
    ImageCoordinates(@NonNull String path) throws IOException{
        this(new ExifInterface(path));
    }

    /**
     * Extracts geolocation (either of image from EXIF data, or of user)
     * @return coordinates as string (needs to be passed as a String in API query)
     */
    @Nullable
    String getDecimalCoords() {
            return decimalCoords;
    }

    public double getDecLatitude() {
        return decLatitude;
    }

    public double getDecLongitude() {
        return decLongitude;
    }

    private double convertToDegree(String stringDMS) {
        String[] DMS = stringDMS.split(",", 3);
        double degrees = divideComponents(DMS[0]);
        double minutes = divideComponents(DMS[1]);
        double seconds = divideComponents(DMS[2]);
        return degrees + (minutes/60) + (seconds/3600);
    }

    private double divideComponents(String dm) {
        String[] stringD = dm.split("/", 2);
        return Double.parseDouble(stringD[0]) / Double.parseDouble(stringD[1]);
    }
}
