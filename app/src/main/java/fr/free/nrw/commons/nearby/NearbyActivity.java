package fr.free.nrw.commons.nearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.theme.NavigationBaseActivity;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.UriSerializer;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;
import uk.co.deanwild.materialshowcaseview.IShowcaseListener;
import uk.co.deanwild.materialshowcaseview.MaterialShowcaseView;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.*;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;


public class NearbyActivity extends NavigationBaseActivity implements LocationUpdateListener,
        WikidataEditListener.WikidataP18EditListener {

    private static final int LOCATION_REQUEST = 1;

    @BindView(R.id.progressBar)
    ProgressBar progressBar;

    @BindView(R.id.bottom_sheet)
    LinearLayout bottomSheet;
    @BindView(R.id.bottom_sheet_details)
    LinearLayout bottomSheetDetails;
    @BindView(R.id.transparentView)
    View transparentView;
    @BindView(R.id.fab_recenter)
    View fabRecenter;

    @Inject
    LocationServiceManager locationManager;
    @Inject
    NearbyController nearbyController;
    @Inject WikidataEditListener wikidataEditListener;

    @Inject
    @Named("application_preferences") SharedPreferences applicationPrefs;
    private LatLng curLatLng;
    private Bundle bundle;
    private Disposable placesDisposable;
    private boolean lockNearbyView; //Determines if the nearby places needs to be refreshed
    private BottomSheetBehavior bottomSheetBehavior; // Behavior for list bottom sheet
    private BottomSheetBehavior bottomSheetBehaviorForDetails; // Behavior for details bottom sheet
    public NearbyMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();
    private View listButton; // Reference to list button to use in tutorial

    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;

    private boolean isListShowcaseAdded = false;
    private boolean isMapShowCaseAdded = false;

    private LatLng lastKnownLocation;

    private MaterialShowcaseView secondSingleShowCaseView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby);
        ButterKnife.bind(this);
        resumeFragment();
        bundle = new Bundle();

        initBottomSheetBehaviour();
        initDrawer();
        wikidataEditListener.setAuthenticationStateListener(this);
    }

    private void resumeFragment() {
        // Find the retained fragment on activity restarts
        nearbyMapFragment = getMapFragment();
        nearbyListFragment = getListFragment();
    }

    private void initBottomSheetBehaviour() {

        transparentView.setAlpha(0);

        bottomSheet.getLayoutParams().height = getWindowManager()
                .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        // TODO initProperBottomSheetBehavior();
        bottomSheetBehavior.setBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {

            @Override
            public void onStateChanged(View bottomSheet, int newState) {
                prepareViewsForSheetPosition(newState);
            }

            @Override
            public void onSlide(View bottomSheet, float slideOffset) {

            }
        });

        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetBehaviorForDetails = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_nearby, menu);

        new Handler().post(() -> {

            listButton = findViewById(R.id.action_display_list);

            secondSingleShowCaseView = new MaterialShowcaseView.Builder(this)
                    .setTarget(listButton)
                    .setDismissText(getString(R.string.showcase_view_got_it_button))
                    .setContentText(getString(R.string.showcase_view_list_icon))
                    .setDelay(500) // optional but starting animations immediately in onCreate can make them choppy
                    .singleUse(ViewUtil.SHOWCASE_VIEW_ID_1) // provide a unique ID used to ensure it is only shown once
                    .setDismissStyle(Typeface.defaultFromStyle(Typeface.BOLD))
                    .setListener(new IShowcaseListener() {
                        @Override
                        public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {

                        }

                        // If dismissed, we can inform fragment to start showcase sequence there
                        @Override
                        public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                                nearbyMapFragment.onNearbyMaterialShowcaseDismissed();
                        }
                    })
                    .build();

            isListShowcaseAdded = true;

            if (isMapShowCaseAdded) { // If map showcase is also ready, start ShowcaseSequence
                // Probably this case is not possible. Just added to be careful
                setMapViewTutorialShowCase();
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_display_list:
                if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
                    bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
                }else if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
                    bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                }

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void requestLocationPermissions() {
        if (!isFinishing()) {
            locationManager.requestPermissions(this);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Timber.d("Location permission granted, refreshing view");
                    //Still need to check if GPS is enabled
                    checkGps();
                    lastKnownLocation = locationManager.getLKL();
                    refreshView(PERMISSION_JUST_GRANTED);
                } else {
                    //If permission not granted, go to page that says Nearby Places cannot be displayed
                    hideProgressBar();
                    showLocationPermissionDeniedErrorDialog();
                }
            }
            break;

            default:
                // This is needed to allow the request codes from the Fragments to be routed appropriately
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void showLocationPermissionDeniedErrorDialog() {
        new AlertDialog.Builder(this)
                .setMessage(R.string.nearby_needs_permissions)
                .setCancelable(false)
                .setPositiveButton(R.string.give_permission, (dialog, which) -> {
                    //will ask for the location permission again
                    checkGps();
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
        if (!locationManager.isProviderEnabled()) {
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
                    .setNegativeButton(R.string.menu_cancel_upload, (dialog, id) -> {
                        showLocationPermissionDeniedErrorDialog();
                        dialog.cancel();
                    })
                    .create()
                    .show();
        } else {
            Timber.d("GPS is enabled");
            checkLocationPermission();
        }
    }

    private void checkLocationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationManager.isLocationPermissionGranted()) {
                refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
            } else {
                // Should we show an explanation?
                if (locationManager.isPermissionExplanationRequired(this)) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestLocationPermissions();
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", (dialog, id) -> {
                                showLocationPermissionDeniedErrorDialog();
                                dialog.cancel();
                            })
                            .create()
                            .show();

                } else {
                    // No explanation needed, we can request the permission.
                    requestLocationPermissions();
                }
            }
        } else {
            refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            Timber.d("User is back from Settings page");
            refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        locationManager.addLocationListener(this);
        registerLocationUpdates();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (placesDisposable != null) {
            placesDisposable.dispose();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        lockNearbyView = false;
        checkGps();
        addNetworkBroadcastReceiver();
    }

    @Override
    public void onPause() {
        super.onPause();
        // this means that this activity will not be recreated now, user is leaving it
        // or the activity is otherwise finishing
        if(isFinishing()) {
            // we will not need this fragment anymore, this may also be a good place to signal
            // to the retained fragment object to perform its own cleanup.
            removeMapFragment();
            removeListFragment();

        }
        unregisterReceiver(broadcastReceiver);
        broadcastReceiver = null;
        locationManager.removeLocationListener(this);
        locationManager.unregisterLocationManager();

    }

    private void addNetworkBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);

        broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (NetworkUtils.isInternetConnectionEstablished(NearbyActivity.this)) {
                    refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
                } else {
                    ViewUtil.showLongToast(NearbyActivity.this, getString(R.string.no_internet));
                }
            }
        };

        this.registerReceiver(broadcastReceiver, intentFilter);
    }


    /**
     * This method should be the single point to load/refresh nearby places
     *
     * @param locationChangeType defines if location shanged significantly or slightly
     */
    private void refreshView(LocationChangeType locationChangeType) {
        if (lockNearbyView) {
            return;
        }

        if (!NetworkUtils.isInternetConnectionEstablished(this)) {
            hideProgressBar();
            return;
        }

        registerLocationUpdates();
        LatLng lastLocation = locationManager.getLastLocation();

        if (curLatLng != null && curLatLng.equals(lastLocation)
                && !locationChangeType.equals(MAP_UPDATED)) { //refresh view only if location has changed
            return;
        }
        curLatLng = lastLocation;

        if (locationChangeType.equals(PERMISSION_JUST_GRANTED)) {
            curLatLng = lastKnownLocation;
        }

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
                || locationChangeType.equals(PERMISSION_JUST_GRANTED)
                || locationChangeType.equals(MAP_UPDATED)) {
            progressBar.setVisibility(View.VISIBLE);

            //TODO: This hack inserts curLatLng before populatePlaces is called (see #1440). Ideally a proper fix should be found
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonCurLatLng = gson.toJson(curLatLng);
            bundle.clear();
            bundle.putString("CurLatLng", gsonCurLatLng);

            placesDisposable = Observable.fromCallable(() -> nearbyController
                    .loadAttractionsFromLocation(curLatLng))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(this::populatePlaces,
                            throwable -> {
                                Timber.d(throwable);
                                showErrorMessage(getString(R.string.error_fetching_nearby_places));
                                progressBar.setVisibility(View.GONE);
                            });
        } else if (locationChangeType
                .equals(LOCATION_SLIGHTLY_CHANGED)) {
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonCurLatLng = gson.toJson(curLatLng);
            bundle.putString("CurLatLng", gsonCurLatLng);
            updateMapFragment(true);
        }
    }

    /**
     * This method first checks if the location permissions has been granted and then register the location manager for updates.
     */
    private void registerLocationUpdates() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationManager.isLocationPermissionGranted()) {
                locationManager.registerLocationManager();
            } else {
                // Should we show an explanation?
                if (locationManager.isPermissionExplanationRequired(this)) {
                    new AlertDialog.Builder(this)
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton("OK", (dialog, which) -> {
                                requestLocationPermissions();
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", (dialog, id) -> {
                                showLocationPermissionDeniedErrorDialog();
                                dialog.cancel();
                            })
                            .create()
                            .show();

                } else {
                    // No explanation needed, we can request the permission.
                    requestLocationPermissions();
                }
            }
        } else {
            locationManager.registerLocationManager();
        }
    }

    private void populatePlaces(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        List<Place> placeList = nearbyPlacesInfo.placeList;
        LatLng[] boundaryCoordinates = nearbyPlacesInfo.boundaryCoordinates;
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriSerializer())
                .create();
        String gsonPlaceList = gson.toJson(placeList);
        String gsonCurLatLng = gson.toJson(curLatLng);
        String gsonBoundaryCoordinates = gson.toJson(boundaryCoordinates);

        if (placeList.size() == 0) {
            ViewUtil.showSnackbar(findViewById(R.id.container), R.string.no_nearby);
        }

        bundle.putString("PlaceList", gsonPlaceList);
        //bundle.putString("CurLatLng", gsonCurLatLng);
        bundle.putString("BoundaryCoord", gsonBoundaryCoordinates);

        // First time to init fragments
        if (nearbyMapFragment == null) {
            Timber.d("Init map fragment for the first time");
            lockNearbyView(true);
            setMapFragment();
            setListFragment();
            hideProgressBar();
            lockNearbyView(false);
        } else {
            // There are fragments, just update the map and list
            Timber.d("Map fragment already exists, just update the map and list");
            updateMapFragment(false);
            updateListFragment();
        }

        isMapShowCaseAdded = true;
    }

    public void setMapViewTutorialShowCase() {
           /*
            *This showcase view will be the first step of our nearbyMaterialShowcaseSequence. The reason we use a
            * single item instead of adding another step to nearbyMaterialShowcaseSequence is that we are not able to
            * call withoutShape() method on steps. For mapView we need an showcase view without
            * any circle on it, it should cover the whole page.
            * */
        MaterialShowcaseView firstSingleShowCaseView = new MaterialShowcaseView.Builder(this)
                .setTarget(nearbyMapFragment.mapView)
                .setDismissText(getString(R.string.showcase_view_got_it_button))
                .setContentText(getString(R.string.showcase_view_whole_nearby_activity))
                .setDelay(500) // optional but starting animations immediately in onCreate can make them choppy
                .singleUse(ViewUtil.SHOWCASE_VIEW_ID_2) // provide a unique ID used to ensure it is only shown once
                .withoutShape() // no shape on map view since there are no view to focus on
                .setDismissStyle(Typeface.defaultFromStyle(Typeface.BOLD))
                .setListener(new IShowcaseListener() {
                    @Override
                    public void onShowcaseDisplayed(MaterialShowcaseView materialShowcaseView) {

                    }

                    @Override
                    public void onShowcaseDismissed(MaterialShowcaseView materialShowcaseView) {
                            /* Add other nearbyMaterialShowcaseSequence here, it will make the user feel as they are a
                            * nearbyMaterialShowcaseSequence whole together.
                            * */
                        secondSingleShowCaseView.show(NearbyActivity.this);
                    }
                })
                .build();

        if (applicationPrefs.getBoolean("firstRunNearby", true)) {
            applicationPrefs.edit().putBoolean("firstRunNearby", false).apply();
            firstSingleShowCaseView.show(this);
        }
    }

    private void lockNearbyView(boolean lock) {
        if (lock) {
            lockNearbyView = true;
            locationManager.unregisterLocationManager();
            locationManager.removeLocationListener(this);
        } else {
            lockNearbyView = false;
            registerLocationUpdates();
            locationManager.addLocationListener(this);
        }
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
        }
    }

    private NearbyMapFragment getMapFragment() {
        return (NearbyMapFragment) getSupportFragmentManager().findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
    }

    private void removeMapFragment() {
        if (nearbyMapFragment != null) {
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().remove(nearbyMapFragment).commit();
            nearbyMapFragment = null;
        }
    }

    private NearbyListFragment getListFragment() {
        return (NearbyListFragment) getSupportFragmentManager().findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
    }

    private void removeListFragment() {
        if (nearbyListFragment != null) {
            android.support.v4.app.FragmentManager fm = getSupportFragmentManager();
            fm.beginTransaction().remove(nearbyListFragment).commit();
            nearbyListFragment = null;
        }
    }

    private void updateMapFragment(boolean isSlightUpdate) {
        /*
        * Significant update means updating nearby place markers. Slightly update means only
        * updating current location marker and camera target.
        * We update our map Significantly on each 1000 meter change, but we can't never know
        * the frequency of nearby places. Thus we check if we are close to the boundaries of
        * our nearby markers, we update our map Significantly.
        * */

        NearbyMapFragment nearbyMapFragment = getMapFragment();

        if (nearbyMapFragment != null && curLatLng != null) {
            hideProgressBar(); // In case it is visible (this happens, not an impossible case)
            /*
            * If we are close to nearby places boundaries, we need a significant update to
            * get new nearby places. Check order is south, north, west, east
            * */
            if (nearbyMapFragment.boundaryCoordinates != null
                    && (curLatLng.getLatitude() <= nearbyMapFragment.boundaryCoordinates[0].getLatitude()
                    || curLatLng.getLatitude() >= nearbyMapFragment.boundaryCoordinates[1].getLatitude()
                    || curLatLng.getLongitude() <= nearbyMapFragment.boundaryCoordinates[2].getLongitude()
                    || curLatLng.getLongitude() >= nearbyMapFragment.boundaryCoordinates[3].getLongitude())) {
                // populate places
                placesDisposable = Observable.fromCallable(() -> nearbyController
                        .loadAttractionsFromLocation(curLatLng))
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe(this::populatePlaces,
                                throwable -> {
                                    Timber.d(throwable);
                                    showErrorMessage(getString(R.string.error_fetching_nearby_places));
                                    progressBar.setVisibility(View.GONE);
                                });
                nearbyMapFragment.setBundleForUpdtes(bundle);
                nearbyMapFragment.updateMapSignificantly();
                updateListFragment();
                return;
            }

            if (isSlightUpdate) {
                nearbyMapFragment.setBundleForUpdtes(bundle);
                nearbyMapFragment.updateMapSlightly();
            } else {
                nearbyMapFragment.setBundleForUpdtes(bundle);
                nearbyMapFragment.updateMapSignificantly();
                updateListFragment();
            }
        } else {
            lockNearbyView(true);
            setMapFragment();
            setListFragment();
            hideProgressBar();
            lockNearbyView(false);
        }
    }

    private void updateListFragment() {
        nearbyListFragment.setBundleForUpdates(bundle);
        nearbyListFragment.updateNearbyListSignificantly();
    }

    /**
     * Calls fragment for map view.
     */
    private void setMapFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        nearbyMapFragment = new NearbyMapFragment();
        nearbyMapFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, nearbyMapFragment, TAG_RETAINED_MAP_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Calls fragment for list view.
     */
    private void setListFragment() {
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        nearbyListFragment = new NearbyListFragment();
        nearbyListFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container_sheet, nearbyListFragment, TAG_RETAINED_LIST_FRAGMENT);
        initBottomSheetBehaviour();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        fragmentTransaction.commitAllowingStateLoss();
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        refreshView(LOCATION_SLIGHTLY_CHANGED);
    }

    public void prepareViewsForSheetPosition(int bottomSheetState) {
        // TODO
    }

    private void showErrorMessage(String message) {
        ViewUtil.showLongToast(NearbyActivity.this, message);
    }

    @Override
    public void onWikidataEditSuccessful() {
        refreshView(MAP_UPDATED);
    }
}
