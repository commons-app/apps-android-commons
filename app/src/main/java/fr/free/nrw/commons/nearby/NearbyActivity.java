package fr.free.nrw.commons.nearby;

import android.content.DialogInterface;
import android.content.Intent;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.UriSerializer;

import fr.free.nrw.commons.R;
import timber.log.Timber;

import java.util.List;

public class NearbyActivity extends BaseActivity {
    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private boolean isMapViewActive = false;

    private LocationServiceManager locationManager;
    private LatLng curLatLang;
    private Gson gson;
    private String gsonPlaceList;
    private String gsonCurLatLng;
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
        checkGps();
        bundle = new Bundle();
        locationManager = new LocationServiceManager(this);
        locationManager.registerLocationManager();
        curLatLang = locationManager.getLatestLocation();
        nearbyAsyncTask = new NearbyAsyncTask();
        nearbyAsyncTask.execute();

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
                                    startActivity(callGPSSettingIntent);
                                }
                            });
            alertDialogBuilder.setNegativeButton("Cancel",
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
    }

    protected void refreshView() {
        nearbyAsyncTask = new NearbyAsyncTask();
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
                    .loadAttractionsFromLocation(curLatLang, getApplicationContext()
                    );
        }

        @Override
        protected void onPostExecute(List<Place> placeList) {
            super.onPostExecute(placeList);

            if (isCancelled()) {
                return;
            }

            gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            gsonPlaceList = gson.toJson(placeList);
            gsonCurLatLng = gson.toJson(curLatLang);

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
}
