package fr.free.nrw.commons.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.inject.Inject;
import javax.inject.Singleton;

import timber.log.Timber;

@Singleton
public class LocationServiceManager implements LocationListener {

    private String provider;
    private LocationManager locationManager;
    private LatLng lastLocation;
    private Float latestLocationAccuracy;
    private final List<LocationUpdateListener> locationListeners = new CopyOnWriteArrayList<>();

    @Inject
    public LocationServiceManager(Context context) {
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        provider = locationManager.getBestProvider(new Criteria(), true);
    }

    public boolean isProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    public LatLng getLastLocation() {
        return lastLocation;
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

    public void addLocationListener(LocationUpdateListener listener) {
        if (!locationListeners.contains(listener)) {
            locationListeners.add(listener);
        }
    }

    public void removeLocationListener(LocationUpdateListener listener) {
        locationListeners.remove(listener);
    }

    @Override
    public void onLocationChanged(Location location) {
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        latestLocationAccuracy = location.getAccuracy();
        Timber.d("Latitude: %f Longitude: %f Accuracy %f",
                currentLatitude, currentLongitude, latestLocationAccuracy);
        lastLocation = new LatLng(currentLatitude, currentLongitude, latestLocationAccuracy);

        for (LocationUpdateListener listener : locationListeners) {
            listener.onLocationChanged(lastLocation);
        }
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
