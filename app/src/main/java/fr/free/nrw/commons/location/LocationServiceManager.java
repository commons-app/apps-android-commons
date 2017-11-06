package fr.free.nrw.commons.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import fr.free.nrw.commons.CommonsApplication;
import timber.log.Timber;

public class LocationServiceManager implements LocationListener {

    private String provider;
    private LocationManager locationManager;
    private LatLng latestLocation;
    private Float latestLocationAccuracy;

    private static LocationServiceManager locationServiceManager;

    private LocationServiceManager() {
        Context applicationContext = CommonsApplication.getInstance().getApplicationContext();
        this.locationManager = (LocationManager) applicationContext.getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
    }

    public static LocationServiceManager getInstance() {
        if (locationServiceManager == null) {
            locationServiceManager = new LocationServiceManager();
        }
        return locationServiceManager;
    }

    public boolean isProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public LatLng getLatestLocation() {
        return latestLocation;
    }

    /**
     * Returns the accuracy of the location. The measurement is
     * given as a radius in meter of 68 % confidence.
     *
     * @return Float
     */
    public Float getLatestLocationAccuracy() {
        return latestLocationAccuracy;
    }

    /** Registers a LocationManager to listen for current location.
     */
    public void registerLocationManager() {
        try {
            locationManager.requestLocationUpdates(provider, 400, 1, this);
            Location location = locationManager.getLastKnownLocation(provider);
            //Location works, just need to 'send' GPS coords
            // via emulator extended controls if testing on emulator
            Timber.d("Checking for location...");
            if (location != null) {
                this.onLocationChanged(location);
            }
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Illegal argument exception");
        } catch (SecurityException e) {
            Timber.e(e, "Security exception");
        }
    }

    /** Unregisters location manager.
     */
    public void unregisterLocationManager() {
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Timber.e(e, "Security exception");
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        latestLocationAccuracy = location.getAccuracy();
        Timber.d("Latitude: %f Longitude: %f Accuracy %f",
                currentLatitude, currentLongitude, latestLocationAccuracy);

        latestLocation = new LatLng(currentLatitude, currentLongitude, latestLocationAccuracy);
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        Timber.d("%s's status changed to %d", provider, status);
    }

    @Override
    public void onProviderEnabled(String provider) {
        Timber.d("Provider %s enabled", provider);
    }

    @Override
    public void onProviderDisabled(String provider) {
        Timber.d("Provider %s disabled", provider);
    }
}
