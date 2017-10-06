package fr.free.nrw.commons.nearby;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.UriSerializer;
import timber.log.Timber;


public class NearbyActivity extends NavigationBaseActivity {

    @BindView(R.id.progressBar)
    ProgressBar progressBar;
    private static final int LOCATION_REQUEST = 1;
    public static final String MAP_LAST_USED_PREFERENCE = "mapLastUsed";

    private LocationServiceManager locationManager;
    private LatLng curLatLang;
    private Bundle bundle;
    private NearbyAsyncTask nearbyAsyncTask;
    private SharedPreferences sharedPreferences;
    private NearbyActivityMode viewMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        setContentView(R.layout.activity_nearby);
        ButterKnife.bind(this);
        checkLocationPermission();
        bundle = new Bundle();
        initDrawer();
        initViewMode();
    }

    private void initViewMode() {
        if (sharedPreferences.getBoolean(MAP_LAST_USED_PREFERENCE, false)) {
            viewMode = NearbyActivityMode.MAP;
        } else {
            viewMode = NearbyActivityMode.LIST;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);

        if (viewMode == NearbyActivityMode.MAP) {
            MenuItem item = menu.findItem(R.id.action_toggle_view);
            item.setIcon(R.drawable.ic_list_white_24dp);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                refreshView();
                return true;
            case R.id.action_toggle_view:
                toggleView();
                toggleIcon(item);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void toggleIcon(MenuItem item) {
        if (viewMode == NearbyActivityMode.MAP) {
            item.setIcon(R.drawable.ic_list_white_24dp);
        } else {
            item.setIcon(R.drawable.ic_map_white_24dp);
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
                if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {

                        // Show an explanation to the user *asynchronously* -- don't block
                        // this thread waiting for the user's response! After the user
                        // sees the explanation, try again to request the permission.

                        new AlertDialog.Builder(this)
                                .setMessage(getString(R.string.location_permission_rationale))
                                .setPositiveButton("OK", (dialog, which) -> {
                                    ActivityCompat.requestPermissions(NearbyActivity.this,
                                            new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                            LOCATION_REQUEST);
                                    dialog.dismiss();
                                })
                                .setNegativeButton("Cancel", null)
                                .create()
                                .show();

                    } else {

                        // No explanation needed, we can request the permission.

                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                LOCATION_REQUEST);

                        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                        // app-defined int constant. The callback method gets the
                        // result of the request.
                    }
                }
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
                    //If permission not granted, go to page that says Nearby Places cannot be displayed
                    if (nearbyAsyncTask != null) {
                        nearbyAsyncTask.cancel(true);
                    }
                    if (progressBar != null) {
                        progressBar.setVisibility(View.GONE);
                    }

                    showLocationPermissionDeniedErrorDialog();
                }
            }
        }
    }

    private void showLocationPermissionDeniedErrorDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.nearby_needs_permissions)
                .setCancelable(false)
                .setPositiveButton(R.string.give_permission, (dialog, which) -> {
                    //will ask for the location permission again
                    checkLocationPermission();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    //dismiss dialog and finish activity
                    dialog.cancel();
                    finish();
                })
                .create()
                .show();
    }

    private void checkGps() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            Timber.d("GPS is not enabled");
            new AlertDialog.Builder(this)
                    .setMessage(R.string.gps_disabled)
                    .setCancelable(false)
                    .setPositiveButton(R.string.enable_gps,
                            (dialog, id) -> {
                                Intent callGPSSettingIntent = new Intent(
                                        android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                                Timber.d("Loaded settings page");
                                startActivityForResult(callGPSSettingIntent, 1);
                            })
                    .setNegativeButton(R.string.menu_cancel_upload, (dialog, id) -> dialog.cancel())
                    .create()
                    .show();
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

    private void toggleView() {
        if (nearbyAsyncTask != null) {
            if (viewMode == NearbyActivityMode.LIST) {
                viewMode = NearbyActivityMode.MAP;
                if (nearbyAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                    setMapFragment();
                    setMapViewLastUsed(true);
                }

            } else {
                viewMode = NearbyActivityMode.LIST;
                if (nearbyAsyncTask.getStatus() == AsyncTask.Status.FINISHED) {
                    setListFragment();
                    setMapViewLastUsed(false);
                }
            }
        }
    }

    private void setMapViewLastUsed(boolean lastUsed) {
        sharedPreferences.edit().putBoolean(MAP_LAST_USED_PREFERENCE, lastUsed).apply();
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

    private void refreshView() {
        nearbyAsyncTask = new NearbyAsyncTask(this);
        nearbyAsyncTask.execute();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (locationManager != null) {
            locationManager.unregisterLocationManager();
        }
    }

    private class NearbyAsyncTask extends AsyncTask<Void, Integer, List<Place>> {

        private final Context mContext;

        private NearbyAsyncTask(Context context) {
            mContext = context;
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
            if (viewMode == NearbyActivityMode.MAP) {
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
    private void setMapFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new NearbyMapFragment();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Calls fragment for list view.
     */
    private void setListFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        Fragment fragment = new NearbyListFragment();
        fragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, fragment);
        fragmentTransaction.commitAllowingStateLoss();
    }

    public static void startYourself(Context context) {
        Intent settingsIntent = new Intent(context, NearbyActivity.class);
        context.startActivity(settingsIntent);
    }
}
