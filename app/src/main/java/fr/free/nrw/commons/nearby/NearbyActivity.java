package fr.free.nrw.commons.nearby;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.UriSerializer;
import timber.log.Timber;


public class NearbyActivity extends NavigationBaseActivity {

    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private boolean isMapViewActive = false;
    private static final int LOCATION_REQUEST = 1;

    private LocationServiceManager locationManager;
    private LatLng curLatLang;
    private Bundle bundle;
    private NearbyAsyncTask nearbyAsyncTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        ButterKnife.bind(this);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        checkLocationPermission();
        bundle = new Bundle();
        initDrawer();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshView();
                return true;
            case R.id.action_map:
                showMapView();
                if (isMapViewActive) {
                    item.setIcon(R.drawable.ic_list_white_24dp);
                } else {
                    item.setIcon(R.drawable.ic_map_white_24dp);
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void startLookingForNearby() {
        locationManager = new LocationServiceManager(this);
        locationManager.registerLocationManager();
        curLatLang = locationManager.getLatestLocation();
        nearbyAsyncTask = new NearbyAsyncTask(this);
        nearbyAsyncTask.execute();
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                startLookingForNearby();
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, LOCATION_REQUEST);
            }
        } else {
            startLookingForNearby();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startLookingForNearby();
                } else {
                    //If permission not granted, display notification that Nearby Places cannot be displayed
                    /**
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(this, R.string.no_location_permission, duration);
                    toast.show();*/

                    //TODO: Open a fragment saying permissions not granted instead
                    FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
                    Fragment fragment = new NoPermissionsFragment();
                    fragment.setArguments(bundle);
                    fragmentTransaction.replace(R.id.container, fragment);
                    fragmentTransaction.commit();
                }
            }
        }
    }

    protected void checkGps() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Timber.d("GPS is not enabled");
            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            alertDialogBuilder.setMessage(R.string.gps_disabled)
                    .setCancelable(false)
                    .setPositiveButton(R.string.enable_gps,
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    Intent callGPSSettingIntent = new Intent(
                                            android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                    Timber.d("Loaded settings page");
                                    startActivityForResult(callGPSSettingIntent, 1);
                                }
                            });
            alertDialogBuilder.setNegativeButton(R.string.menu_cancel_upload,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });
            AlertDialog alert = alertDialogBuilder.create();
            alert.show();
        } else {
            Timber.d("GPS is enabled");
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            Timber.d("User is back from Settings page");
            refreshView();
        }
    }

    private void showMapView() {
        if (!isMapViewActive) {
            isMapViewActive = true;
            if (nearbyAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                setMapFragment();
            }

        } else {
            isMapViewActive = false;
            if (nearbyAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                setListFragment();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGps();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nearbyAsyncTask != null) {
            nearbyAsyncTask.cancel(true);
        }
    }

    protected void refreshView() {
        nearbyAsyncTask = new NearbyAsyncTask(this);
        nearbyAsyncTask.execute();
    }

    public LocationServiceManager getLocationManager() {
        return locationManager;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        locationManager.unregisterLocationManager();
    }

    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        private Context mContext;

        private NearbyAsyncTask (Context context) {
            mContext = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected List<Place> doInBackground(Void... params) {
            return NearbyController
                    .loadAttractionsFromLocation(curLatLang, CommonsApplication.getInstance()
                    );
        }

        @Override
        protected void onPostExecute(List<Place> placeList) {
            super.onPostExecute(placeList);

            if (isCancelled()) {
                return;
            }

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonPlaceList = gson.toJson(placeList);
            String gsonCurLatLng = gson.toJson(curLatLang);

            if (placeList.size() == 0) {
                int duration = Toast.LENGTH_SHORT;
                Toast toast = Toast.makeText(mContext, R.string.no_nearby, duration);
                toast.show();
            }

            bundle.clear();
            bundle.putString("PlaceList", gsonPlaceList);
            bundle.putString("CurLatLng", gsonCurLatLng);

            // Begin the transaction
            if (isMapViewActive) {
                setMapFragment();
            } else {
                setListFragment();
            }

            if (progressBar != null) {
                progressBar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Calls fragment for map view.
     */
    public void setMapFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new NearbyMapFragment();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commit();
    }

    /**
     * Calls fragment for list view.
     */
    public void setListFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new NearbyListFragment();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commit();
    }

    public static void startYourself(Context context) {
        Intent settingsIntent = new Intent(context, NearbyActivity.class);
        context.startActivity(settingsIntent);
    }
}
