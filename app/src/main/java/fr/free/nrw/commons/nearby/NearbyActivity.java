package fr.free.nrw.commons.nearby;

import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.BaseActivity;
import fr.free.nrw.commons.utils.UriSerializer;

import fr.free.nrw.commons.R;

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
        nearbyAsyncTask = new NearbyAsyncTask(this);
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

            if (placeList.size() == 0) {
                CharSequence text = "No nearby places found";
                int duration = Toast.LENGTH_SHORT;

                Toast toast = Toast.makeText(mContext, text, duration);
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
}
