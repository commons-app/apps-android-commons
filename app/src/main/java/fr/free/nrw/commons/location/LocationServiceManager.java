package fr.free.nrw.commons.location;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import timber.log.Timber;

public class LocationServiceManager implements LocationListener {

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
        Timber.d("Latitude: %f Longitude: %f", currentLatitude, currentLongitude);

        latestLocation = new LatLng(currentLatitude, currentLongitude);
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
