package fr.free.nrw.commons.location;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import timber.log.Timber;

public class LocationServiceManager implements LocationListener {
    public static final int LOCATION_REQUEST = 1;

    // Maybe these values can be improved for efficiency
    private static final long MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS = 2 * 60 * 100;
    private static final long MIN_LOCATION_UPDATE_REQUEST_DISTANCE_IN_METERS = 10;

    private Context context;
    private LocationManager locationManager;
    private Location lastLocation;
    private final List<LocationUpdateListener> locationListeners = new CopyOnWriteArrayList<>();
    private boolean isLocationManagerRegistered = false;
    private Set<Activity> locationExplanationDisplayed = new HashSet<>();

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

    /**
     * The permission explanation dialog box is now displayed just once for a particular activity. We are subscribing
     * to updates from multiple providers so its important to show the dialog just once. Otherwise it will be displayed
     * once for every provider, which in our case currently is 2.
     * @param activity
     * @return
     */
    public boolean isPermissionExplanationRequired(Activity activity) {
        if (activity.isFinishing()) {
            return false;
        }
        boolean showRequestPermissionRationale = ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.ACCESS_FINE_LOCATION);
        if (showRequestPermissionRationale && !locationExplanationDisplayed.contains(activity)) {
            locationExplanationDisplayed.add(activity);
            return true;
        }
        return false;
    }

    /**
     * Gets the last known location in cases where there wasn't time to register a listener
     * (e.g. when Location permission just granted)
     * @return last known LatLng
     */
    @SuppressLint("MissingPermission")
    public LatLng getLKL() {
        if (isLocationPermissionGranted()) {
            Location lastKL = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKL == null) {
                lastKL = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (lastKL == null) {
                return null;
            }
            return LatLng.from(lastKL);
        } else {
            return null;
        }
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
        if (!isLocationManagerRegistered) {
            isLocationManagerRegistered = requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER)
                    && requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
        }
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
     * @return LOCATION_SIGNIFICANTLY_CHANGED if location changed significantly
     * LOCATION_SLIGHTLY_CHANGED if location changed slightly
     */
    private LocationChangeType isBetterLocation(Location location, Location currentBestLocation) {

        if (currentBestLocation == null) {
            // A new location is always better than no location
            return LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
        }

        // Check whether the new location fix is newer or older
        long timeDelta = location.getTime() - currentBestLocation.getTime();
        boolean isSignificantlyNewer = timeDelta > MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS;
        boolean isSignificantlyOlder = timeDelta < -MIN_LOCATION_UPDATE_REQUEST_TIME_IN_MILLIS;
        boolean isNewer = timeDelta > 0;

        // Check whether the new location fix is more or less accurate
        int accuracyDelta = (int) (location.getAccuracy() - currentBestLocation.getAccuracy());
        boolean isLessAccurate = accuracyDelta > 0;
        boolean isMoreAccurate = accuracyDelta < 0;
        boolean isSignificantlyLessAccurate = accuracyDelta > 200;

        // Check if the old and new location are from the same provider
        boolean isFromSameProvider = isSameProvider(location.getProvider(),
                currentBestLocation.getProvider());

        float[] results = new float[5];
        Location.distanceBetween(
                        currentBestLocation.getLatitude(),
                        currentBestLocation.getLongitude(),
                        location.getLatitude(),
                        location.getLongitude(),
                        results);

        // If it's been more than two minutes since the current location, use the new location
        // because the user has likely moved
        if (isSignificantlyNewer
                || isMoreAccurate
                || (isNewer && !isLessAccurate)
                || (isNewer && !isSignificantlyLessAccurate && isFromSameProvider)) {
            if (results[0] < 1000) { // Means change is smaller than 1000 meter
                return LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
            } else {
                return LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
            }
        } else{
            return LocationChangeType.LOCATION_NOT_CHANGED;
        }
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
            if (isBetterLocation(location, lastLocation)
                    .equals(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)) {
                lastLocation = location;
                for (LocationUpdateListener listener : locationListeners) {
                    listener.onLocationChangedSignificantly(LatLng.from(lastLocation));
                }
            } else if (isBetterLocation(location, lastLocation)
                    .equals(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)) {
                lastLocation = location;
                for (LocationUpdateListener listener : locationListeners) {
                    listener.onLocationChangedSlightly(LatLng.from(lastLocation));
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

    public enum LocationChangeType{
        LOCATION_SIGNIFICANTLY_CHANGED, //Went out of borders of nearby markers
        LOCATION_SLIGHTLY_CHANGED,      //User might be walking or driving
        LOCATION_NOT_CHANGED,
        PERMISSION_JUST_GRANTED,
        MAP_UPDATED
    }
}
