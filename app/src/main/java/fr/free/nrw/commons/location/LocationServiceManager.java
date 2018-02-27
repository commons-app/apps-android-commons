package fr.free.nrw.commons.location;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public class LocationServiceManager implements LocationListener {
    public static final int LOCATION_REQUEST = 1;

    private static final long MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS = 2 * 60 * 1000;
    private static final long MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS = 10;

    private Context context;
    private LocationManager locationManager;
    private Location lastLocation;
    private final List<LocationUpdateListener> locationListeners = new CopyOnWriteArrayList<>();
    private boolean isLocationManagerRegistered = false;

    /**
     * Constructs a new instance of LocationServiceManager.
     *
     * @param context the context
     */
    public LocationServiceManager(Context context) {
        this.context = context;
        this.locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    /**
     * Returns the current status of the GPS provider.
     *
     * @return true if the GPS provider is enabled
     */
    public boolean isProviderEnabled() {
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }

    /**
     * Returns whether the location permission is granted.
     *
     * @return true if the location permission is granted
     */
    public boolean isLocationPermissionGranted() {
        return ContextCompat.checkSelfPermission(context,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Requests the location permission to be granted.
     *
     * @param activity the activity
     */
    public void requestPermissions(Activity activity) {
        if (activity.isFinishing()) {
            return;
        }
        ActivityCompat.requestPermissions(activity,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_REQUEST);
    }

    public boolean isPermissionExplanationRequired(Activity activity) {
        return !activity.isFinishing() &&
                ActivityCompat.shouldShowRequestPermissionRationale(activity,
                        Manifest.permission.ACCESS_FINE_LOCATION);
    }

    public LatLng getLastLocation() {
        if (lastLocation == null) {
            return null;
        }
        return LatLng.from(lastLocation);
    }

    /**
     * Registers a LocationManager to listen for current location.
     */
    public void registerLocationManager() {
        if (!isLocationManagerRegistered)
            isLocationManagerRegistered = requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
                    && requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
    }

    /**
     * Requests location updates from the specified provider.
     *
     * @param locationProvider the location provider
     * @return true if successful
     */
    private boolean requestLocationUpdatesFromProvider(String locationProvider) {
        try {
            locationManager.requestLocationUpdates(locationProvider,
                    MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS,
                    MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS,
                    this);
            return true;
        } catch (IllegalArgumentException e) {
            Timber.e(e, "Illegal argument exception");
            return false;
        } catch (SecurityException e) {
            Timber.e(e, "Security exception");
            return false;
        }
    }

    /**
     * Returns whether a given location is better than the current best location.
     *
     * @param location            the location to be tested
     * @param currentBestLocation the current best location
     * @return true if the given location is better
     */
    protected boolean isBetterLocation(Location location, Location currentBestLocation) {
        if (currentBestLocation == null) {
            // A new location is always better than no location
            return true;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS;
        boolean isSignificantlyOlder = timeDelta < -MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS;
        boolean isNewer = timeDelta > 0;

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer) {
            return true;
            // If the new location is more than two minutes older, it must be worse
        } else if (isSignificantlyOlder) {
            return false;
        }

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        // Determine location quality using a combination of timeliness and accuracy
        if (isMoreAccurate) {
            return true;
        } else if (isNewer && !isLessAccurate) {
            return true;
        } else if (isNewer && !isSignificantlyLessAccurate && isFromSameProvider) {
            return true;
        }
        return false;
    }

    /**
     * Checks whether two providers are the same
     */
    private boolean isSameProvider(String provider1, String provider2) {
        if (provider1 == null) {
            return provider2 == null;
        }
        return provider1.equals(provider2);
    }

    /**
     * Unregisters location manager.
     */
    public void unregisterLocationManager() {
        isLocationManagerRegistered = false;
        try {
            locationManager.removeUpdates(this);
        } catch (SecurityException e) {
            Timber.e(e, "Security exception");
        }
    }

    /**
     * Adds a new listener to the list of location listeners.
     *
     * @param listener the new listener
     */
    public void addLocationListener(LocationUpdateListener listener) {
        if (!locationListeners.contains(listener)) {
            locationListeners.add(listener);
        }
    }

    /**
     * Removes a listener from the list of location listeners.
     *
     * @param listener the listener to be removed
     */
    public void removeLocationListener(LocationUpdateListener listener) {
        locationListeners.remove(listener);
    }

    @Override
    public void onLocationChanged(Location location) {
        if (isBetterLocation(location, lastLocation)) {
            lastLocation = location;
            for (LocationUpdateListener listener : locationListeners) {
                listener.onLocationChanged(LatLng.from(lastLocation));
            }
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
