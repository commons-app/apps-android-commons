package fr.free.nrw.commons.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

public class LocationServiceManager implements LocationListener {
    public static final String TAG = "LocationServiceManager";
    private String provider;
    private LocationManager locationManager;
    private LatLng latestLocation;

    public LocationServiceManager(Context context) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
    }

    public LatLng getLatestLocation() {
        return latestLocation;
    }

    /** Registers a LocationManager to listen for current location.
     */
    public void registerLocationManager() {
        try {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
            Location location = locationManager.getLastKnownLocation(provider);
            //Location works, just need to 'send' GPS coords
            // via emulator extended controls if testing on emulator
            Log.d(TAG, "Checking for location...");
            if (location != null) {
                this.onLocationChanged(location);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal argument exception", e);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
        }
    }

    /** Unregisters location manager.
     */
    public void unregisterLocationManager() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception", e);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        Log.d(TAG, "Latitude: " + String.valueOf(currentLatitude)
                + " Longitude: " + String.valueOf(currentLongitude));

        latestLocation = new LatLng(currentLatitude, currentLongitude);
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
