package fr.free.nrw.commons.upload;

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.util.Log;
import java.io.IOException;

/**
 * Extracts geolocation to be passed to API for category suggestions. If a picture with geolocation
 * is uploaded, extract latitude and longitude from EXIF data of image. If a picture without
 * geolocation is uploaded, retrieve user's location (if enabled in Settings).
 */
public class GPSExtractor {

    private static final String TAG = GPSExtractor.class.getName();

    private String filePath;
    private double decLatitude, decLongitude;
    private double currentLatitude, currentLongitude;
    private Context context;
    public boolean imageCoordsExists;
    private MyLocationListener myLocationListener;
    private LocationManager locationManager;
    private String provider;
    private Criteria criteria;

    public GPSExtractor(String filePath, Context context){
        this.filePath = filePath;
        this.context = context;
    }

    /**
     * Check if user enabled retrieval of their current location in Settings
     * @return true if enabled, false if disabled
     */
    private boolean gpsPreferenceEnabled() {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        boolean gpsPref = sharedPref.getBoolean("allowGps", false);
        Log.d(TAG, "Gps pref set to: " + gpsPref);
        return gpsPref;
    }

    /**
     * Registers a LocationManager to listen for current location
     */
    protected void registerLocationManager() {
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        myLocationListener = new MyLocationListener();

        try {
            locationManager.requestLocationUpdates(provider, 400, 1, myLocationListener);
            Location location = locationManager.getLastKnownLocation(provider);
            if (location != null) {
                myLocationListener.onLocationChanged(location);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
        }
    }

    protected void unregisterLocationManager() {
        try {
            locationManager.removeUpdates(myLocationListener);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
        }
    }

    /**
     * Extracts geolocation of image from EXIF data.
     * @return coordinates of image as string (needs to be passed as a String in API query)
     */
    @Nullable
    public String getCoords(boolean useGPS) {

        ExifInterface exif;
        String latitude = "";
        String longitude = "";
        String latitude_ref = "";
        String longitude_ref = "";
        String decimalCoords = "";

        try {
            exif = new ExifInterface(filePath);
        } catch (IOException e) {
            Log.w("Image", e);
            return null;
        } catch (IllegalArgumentException e) {
            Log.w("Image", e);
            return null;
        }

        if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null && useGPS) {
            registerLocationManager();

            imageCoordsExists = false;
            Log.d(TAG, "EXIF data has no location info");

            //Check what user's preference is for automatic location detection
            boolean gpsPrefEnabled = gpsPreferenceEnabled();

            if (gpsPrefEnabled) {
                Log.d(TAG, "Current location values: Lat = " + currentLatitude + " Long = " + currentLongitude);
                return String.valueOf(currentLatitude) + "|" + String.valueOf(currentLongitude);
            } else {
                // No coords found
                return null;
            }
        } else if (exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE) == null) {
            return null;
        } else {
            imageCoordsExists = true;
            Log.d(TAG, "EXIF data has location info");

            latitude = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            latitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            longitude = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            longitude_ref = exif.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (latitude!=null && latitude_ref!=null && longitude!=null && longitude_ref!=null) {
                Log.d("Image", "Latitude: " + latitude + " " + latitude_ref);
                Log.d("Image", "Longitude: " + longitude + " " + longitude_ref);

                decimalCoords = getDecimalCoords(latitude, latitude_ref, longitude, longitude_ref);
                return decimalCoords;
            } else {
                return null;
            }
        }
    }

    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            Log.d(TAG, provider + "'s status changed to " + status);
        }

        @Override
        public void onProviderEnabled(String provider) {
            Log.d(TAG, "Provider " + provider + " enabled");
        }

        @Override
        public void onProviderDisabled(String provider) {
            Log.d(TAG, "Provider " + provider + " disabled");
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
        Log.d("Coords", "Latitude and Longitude are " + decimalCoords);
        return decimalCoords;
    }

    private double convertToDegree(String stringDMS){
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
