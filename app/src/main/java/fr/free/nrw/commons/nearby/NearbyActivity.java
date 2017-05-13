package fr.free.nrw.commons.nearby;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
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
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.UriSerializer;

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
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void showMapView() {
        if (!isMapViewActive) {
            setMapFragment();
            isMapViewActive = true;
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
     * Calls fragment for map view
     */
    public void setMapFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        NearbyMapFragment fragment = new NearbyMapFragment();
        fragment.setArguments(bundle);
        ft.add(R.id.container, fragment);
        ft.commit();
    }

    /**
     * Calls fragment for list view
     */
    public void setListFragment() {
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        NearbyListFragment fragment = new NearbyListFragment();
        fragment.setArguments(bundle);
        ft.add(R.id.container, fragment);
        ft.commit();
    }

}
