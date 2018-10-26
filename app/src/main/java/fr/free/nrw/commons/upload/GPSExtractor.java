package fr.free.nrw.commons.upload;

import android.media.ExifInterface;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.IOException;

import timber.log.Timber;

/**
 * Extracts geolocation to be passed to API for category suggestions. If a picture with geolocation
 * is uploaded, extract latitude and longitude from EXIF data of image.
 */
public class GPSExtractor {

    private ExifInterface exif;
    private double decLatitude;
    private double decLongitude;
    public boolean imageCoordsExists;

    /**
     * Construct from the file descriptor of the image (only for API 24 or newer).
     * @param fileDescriptor the file descriptor of the image
     */
    @RequiresApi(24)
    public GPSExtractor(@NonNull FileDescriptor fileDescriptor) {
        try {
            exif = new ExifInterface(fileDescriptor);
        } catch (IOException | IllegalArgumentException e) {
            Timber.w(e);
        }
    }

    /**
     * Construct from the file path of the image.
     * @param path file path of the image
     *
     */
    public GPSExtractor(@NonNull String path) {
        try {
            exif = new ExifInterface(path);
        } catch (IOException | IllegalArgumentException e) {
            Timber.w(e);
        }
    }

    /**
     * Extracts geolocation (either of image from EXIF data, or of user)
     * @return coordinates as string (needs to be passed as a String in API query)
     */
    @Nullable
    public String getCoords() {
        String latitude;
        String longitude;
        String latitudeRef;
        String longitudeRef;
        String decimalCoords;

        //If image has no EXIF data and user has enabled GPS setting, get user's location
        //TODO: Always return null as a temporary fix for #1599
        if (exif == null || exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null) {
            return null;
        } else {
            //If image has EXIF data, extract image coords
            imageCoordsExists = true;
            Timber.d("EXIF data has location info");

            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longitudeRef = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (latitude!=null && latitudeRef!=null && longitude!=null && longitudeRef!=null) {
                Timber.d("Latitude: %s %s", latitude, latitudeRef);
                Timber.d("Longitude: %s %s", longitude, longitudeRef);

                decimalCoords = getDecimalCoords(latitude, latitudeRef, longitude, longitudeRef);
                return decimalCoords;
            } else {
                return null;
            }
        }
    }

    public double getDecLatitude() {
        return decLatitude;
    }

    public double getDecLongitude() {
        return decLongitude;
    }

    /**
     * Converts format of geolocation into decimal coordinates as required by MediaWiki API
     * @return the coordinates in decimals
     */
    private String getDecimalCoords(String latitude, String latitude_ref, String longitude, String longitude_ref) {

        if (latitude_ref.equals("N")) {
            decLatitude = convertToDegree(latitude);
        } else {
            decLatitude = 0 - convertToDegree(latitude);
        }

        if (longitude_ref.equals("E")) {
            decLongitude = convertToDegree(longitude);
        } else {
            decLongitude = 0 - convertToDegree(longitude);
        }

        String decimalCoords = String.valueOf(decLatitude) + "|" + String.valueOf(decLongitude);
        Timber.d("Latitude and Longitude are %s", decimalCoords);
        return decimalCoords;
    }

    private double convertToDegree(String stringDMS) {
        double result;
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        double d0 = Double.parseDouble(stringD[0]);
        double d1 = Double.parseDouble(stringD[1]);
        double degrees = d0/d1;

        String[] stringM = DMS[1].split("/", 2);
        double m0 = Double.parseDouble(stringM[0]);
        double m1 = Double.parseDouble(stringM[1]);
        double minutes = m0/m1;

        String[] stringS = DMS[2].split("/", 2);
        double s0 = Double.parseDouble(stringS[0]);
        double s1 = Double.parseDouble(stringS[1]);
        double seconds = s0/s1;

        result = degrees + (minutes/60) + (seconds/3600);
        return result;
    }
}
