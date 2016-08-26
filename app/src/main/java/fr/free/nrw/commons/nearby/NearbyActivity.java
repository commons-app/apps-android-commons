package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import fr.free.nrw.commons.R;

public class NearbyActivity extends AppCompatActivity {

    private MyLocationListener myLocationListener;
    private LocationManager locationManager;
    private String provider;
    private Criteria criteria;

    private double currentLatitude, currentLongitude;
    //private String gpsCoords;

    private static final String TAG = "NearbyActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        // Begin the transaction
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        NearbyListFragment fragment = new NearbyListFragment();
        ft.add(R.id.container, fragment);
        ft.commit();

        /*
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new NearbyListFragment())
                    .commit();
        }
        */
    }

    @Override
    protected void onResume(){
        super.onResume();

        registerLocationManager();
        //TODO: Check if we need String or double coords, and in what format
        //gpsCoords = String.valueOf(currentLatitude) + "|" + String.valueOf(currentLongitude);
    }


    /**
     * Registers a LocationManager to listen for current location
     */
    protected void registerLocationManager() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        criteria = new Criteria();
        provider = locationManager.getBestProvider(criteria, true);
        myLocationListener = new MyLocationListener();

        try {
            locationManager.requestLocationUpdates(provider, 400, 1, myLocationListener);
            Location location = locationManager.getLastKnownLocation(provider);
            Log.d(TAG, "Checking for location...");
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
     * Listen for user's location when it changes
     */
    private class MyLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            currentLatitude = location.getLatitude();
            currentLongitude = location.getLongitude();
            Log.d(TAG, "Latitude: " + String.valueOf(currentLatitude) + " Longitude: " + String.valueOf(currentLongitude));
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

    @Override
    protected void onDestroy(){
        super.onDestroy();

        unregisterLocationManager();
    }
}
