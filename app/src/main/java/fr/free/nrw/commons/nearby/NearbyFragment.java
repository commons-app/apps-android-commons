package fr.free.nrw.commons.nearby;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
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
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.UriSerializer;
import fr.free.nrw.commons.utils.ViewUtil;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.PERMISSION_JUST_GRANTED;

public class NearbyFragment extends CommonsDaggerSupportFragment
        implements LocationUpdateListener,
                    WikidataEditListener.WikidataP18EditListener {

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
    @Inject
    WikidataEditListener wikidataEditListener;
    @Inject
    @Named("application_preferences")
    SharedPreferences applicationPrefs;

    public NearbyMapFragment nearbyMapFragment;
    private NearbyListFragment nearbyListFragment;
    private static final String TAG_RETAINED_MAP_FRAGMENT = NearbyMapFragment.class.getSimpleName();
    private static final String TAG_RETAINED_LIST_FRAGMENT = NearbyListFragment.class.getSimpleName();
    private Bundle bundle;
    private BottomSheetBehavior bottomSheetBehavior; // Behavior for list bottom sheet
    private BottomSheetBehavior bottomSheetBehaviorForDetails; // Behavior for details bottom sheet

    private LatLng curLatLng;
    private Disposable placesDisposable;
    private boolean lockNearbyView; //Determines if the nearby places needs to be refreshed
    public View view;
    private Snackbar snackbar;

    private LatLng lastKnownLocation;

    private final String NETWORK_INTENT_ACTION = "android.net.conn.CONNECTIVITY_CHANGE";
    private BroadcastReceiver broadcastReceiver;

    private boolean onOrientationChanged = false;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_nearby, container, false);
        ButterKnife.bind(this, view);

        /*// Resume the fragment if exist
        resumeFragment();*/
        bundle = new Bundle();
        initBottomSheetBehaviour();
        this.view = view;
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (savedInstanceState != null) {
            onOrientationChanged = true;
            refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    /**
     * Hide or expand bottom sheet according to states of all sheets
     */
    public void listOptionMenuIteClicked() {
        if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_COLLAPSED || bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_HIDDEN){
            bottomSheetBehaviorForDetails.setState(BottomSheetBehavior.STATE_HIDDEN);
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
        }else if(bottomSheetBehavior.getState()==BottomSheetBehavior.STATE_EXPANDED){
            bottomSheetBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
        }

    }

    /**
     * Resume fragments if they exists
     */
    private void resumeFragment() {
        // Find the retained fragment on activity restarts
        nearbyMapFragment = getMapFragment();
        nearbyListFragment = getListFragment();
    }

    /**
     * Returns the map fragment added to child fragment manager previously, if exists.
     */
    private NearbyMapFragment getMapFragment() {
        return (NearbyMapFragment) getChildFragmentManager().findFragmentByTag(TAG_RETAINED_MAP_FRAGMENT);
    }

    private void removeMapFragment() {
        if (nearbyMapFragment != null) {
            android.support.v4.app.FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(nearbyMapFragment).commit();
            nearbyMapFragment = null;
        }
    }


    /**
     * Returns the list fragment added to child fragment manager previously, if exists.
     */
    private NearbyListFragment getListFragment() {
        return (NearbyListFragment) getChildFragmentManager().findFragmentByTag(TAG_RETAINED_LIST_FRAGMENT);
    }

    private void removeListFragment() {
        if (nearbyListFragment != null) {
            android.support.v4.app.FragmentManager fm = getFragmentManager();
            fm.beginTransaction().remove(nearbyListFragment).commit();
            nearbyListFragment = null;
        }
    }

    /**
     * Initialize bottom sheet behaviour (sheet for map list.) Set height 9/16 of all window.
     * Add callback for bottom sheet changes, so that we can sync it with bottom sheet for details
     * (sheet for nearby details)
     */
    private void initBottomSheetBehaviour() {

        transparentView.setAlpha(0);
        bottomSheet.getLayoutParams().height = getActivity().getWindowManager()
                .getDefaultDisplay().getHeight() / 16 * 9;
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
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

    public void prepareViewsForSheetPosition(int bottomSheetState) {
        // TODO
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        refreshView(LOCATION_SLIGHTLY_CHANGED);
    }


    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        // For nearby map actions, there are no differences between 500 meter location change (aka medium change) and slight change
        refreshView(LOCATION_SLIGHTLY_CHANGED);
    }

    @Override
    public void onWikidataEditSuccessful() {
        refreshView(MAP_UPDATED);
    }

    /**
     * This method should be the single point to load/refresh nearby places
     *
     * @param locationChangeType defines if location shanged significantly or slightly
     */
    private void refreshView(LocationServiceManager.LocationChangeType locationChangeType) {
        Timber.d("Refreshing nearby places");
        if (lockNearbyView) {
            return;
        }

        if (!NetworkUtils.isInternetConnectionEstablished(getActivity())) {
            hideProgressBar();
            return;
        }

        registerLocationUpdates();
        LatLng lastLocation = locationManager.getLastLocation();

        if (curLatLng != null && curLatLng.equals(lastLocation)
                && !locationChangeType.equals(MAP_UPDATED)) { //refresh view only if location has changed
            if (!onOrientationChanged) {
                return;
            }
        }
        curLatLng = lastLocation;

        if (locationChangeType.equals(PERMISSION_JUST_GRANTED)) {
            curLatLng = lastKnownLocation;
        }

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        /*
        onOrientation changed is true whenever activities orientation changes. After orientation
        change we want to refresh map significantly, doesn't matter if location changed significantly
        or not. Thus, we included onOrientatinChanged boolean to if clause
         */
        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
                || locationChangeType.equals(PERMISSION_JUST_GRANTED)
                || locationChangeType.equals(MAP_UPDATED)
                || onOrientationChanged) {
            progressBar.setVisibility(View.VISIBLE);

            //TODO: This hack inserts curLatLng before populatePlaces is called (see #1440). Ideally a proper fix should be found
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(Uri.class, new UriSerializer())
                    .create();
            String gsonCurLatLng = gson.toJson(curLatLng);
            bundle.clear();
            bundle.putString("CurLatLng", gsonCurLatLng);

            placesDisposable = Observable.fromCallable(() -> nearbyController
                    .loadAttractionsFromLocation(curLatLng, false))
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

    private void populatePlaces(NearbyController.NearbyPlacesInfo nearbyPlacesInfo) {
        Timber.d("Populating nearby places");
        List<Place> placeList = nearbyPlacesInfo.placeList;
        LatLng[] boundaryCoordinates = nearbyPlacesInfo.boundaryCoordinates;
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Uri.class, new UriSerializer())
                .create();
        String gsonPlaceList = gson.toJson(placeList);
        String gsonCurLatLng = gson.toJson(curLatLng);
        String gsonBoundaryCoordinates = gson.toJson(boundaryCoordinates);

        if (placeList.size() == 0) {
            ViewUtil.showSnackbar(view.findViewById(R.id.container), R.string.no_nearby);
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
    }

    /**
     * Lock nearby view updates while updating map or list. Because we don't want new update calls
     * when we already updating for old location update.
     * @param lock
     */
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

    private void updateMapFragment(boolean isSlightUpdate) {
        /*
        Significant update means updating nearby place markers. Slightly update means only
        updating current location marker and camera target.
        We update our map Significantly on each 1000 meter change, but we can't never know
        the frequency of nearby places. Thus we check if we are close to the boundaries of
        our nearby markers, we update our map Significantly.
         */
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
                        .loadAttractionsFromLocation(curLatLng, false))
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

            /*
            If this is the map update just after orientation change, then it is not a slight update
            anymore. We want to significantly update map after each orientation change
             */
            if (onOrientationChanged) {
                isSlightUpdate = false;
                onOrientationChanged = false;
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
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        nearbyMapFragment = new NearbyMapFragment();
        nearbyMapFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container, nearbyMapFragment, TAG_RETAINED_MAP_FRAGMENT);
        fragmentTransaction.commitAllowingStateLoss();
    }

    /**
     * Calls fragment for list view.
     */
    private void setListFragment() {
        FragmentTransaction fragmentTransaction = getChildFragmentManager().beginTransaction();
        nearbyListFragment = new NearbyListFragment();
        nearbyListFragment.setArguments(bundle);
        fragmentTransaction.replace(R.id.container_sheet, nearbyListFragment, TAG_RETAINED_LIST_FRAGMENT);
        initBottomSheetBehaviour();
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        fragmentTransaction.commitAllowingStateLoss();
    }

    private void hideProgressBar() {
        if (progressBar != null) {
            progressBar.setVisibility(View.GONE);
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
                if (locationManager.isPermissionExplanationRequired(getActivity())) {
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                requestLocationPermissions();
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
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

    private void requestLocationPermissions() {
        if (!getActivity().isFinishing()) {
            locationManager.requestPermissions(getActivity());
        }
    }

    private void showLocationPermissionDeniedErrorDialog() {
        new AlertDialog.Builder(getActivity())
                .setMessage(R.string.nearby_needs_permissions)
                .setCancelable(false)
                .setPositiveButton(R.string.give_permission, (dialog, which) -> {
                    //will ask for the location permission again
                    checkGps();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> {
                    //dismiss dialog and send user to contributions tab instead
                    dialog.cancel();
                    ((MainActivity)getActivity()).viewPager.setCurrentItem(((MainActivity)getActivity()).CONTRIBUTIONS_TAB_POSITION);
                })
                .create()
                .show();
    }

    /**
     * Checks device GPS permission first for all API levels
     */
    private void checkGps() {
        Timber.d("checking GPS");
        if (!locationManager.isProviderEnabled()) {
            Timber.d("GPS is not enabled");
            new AlertDialog.Builder(getActivity())
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

    /**
     * This method ideally should be called from inside of CheckGPS method. If device GPS is enabled
     * then we need to control app specific permissions for >=M devices. For other devices, enabled
     * GPS is enough for nearby, so directly call refresh view.
     */
    private void checkLocationPermission() {
        Timber.d("Checking location permission");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (locationManager.isLocationPermissionGranted()) {
                refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
            } else {
                // Should we show an explanation?
                if (locationManager.isPermissionExplanationRequired(getActivity())) {
                    // Show an explanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.
                    new AlertDialog.Builder(getActivity())
                            .setMessage(getString(R.string.location_permission_rationale_nearby))
                            .setPositiveButton(android.R.string.ok, (dialog, which) -> {
                                requestLocationPermissions();
                                dialog.dismiss();
                            })
                            .setNegativeButton(android.R.string.cancel, (dialog, id) -> {
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

    private void showErrorMessage(String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    private void addNetworkBroadcastReceiver() {
        IntentFilter intentFilter = new IntentFilter(NETWORK_INTENT_ACTION);
            snackbar = Snackbar.make(transparentView , R.string.no_internet, Snackbar.LENGTH_INDEFINITE);

            broadcastReceiver = new BroadcastReceiver() {

            @Override
            public void onReceive(Context context, Intent intent) {
                if (snackbar != null) {
                    if (NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                        refreshView(LOCATION_SIGNIFICANTLY_CHANGED);
                        snackbar.dismiss();
                    } else {
                        snackbar.show();
                    }
                }
            }
        };

        getActivity().registerReceiver(broadcastReceiver, intentFilter);

    }

    @Override
    public void onResume() {
        super.onResume();
        // Resume the fragment if exist
        resumeFragment();
        }

    public void onTabSelected(boolean onOrientationChanged) {
        Timber.d("On nearby tab selected");
        this.onOrientationChanged = onOrientationChanged;
        performNearbyOperations();

    }

    /**
     * Calls nearby operations in required order.
     */
    private void performNearbyOperations() {
        locationManager.addLocationListener(this);
        registerLocationUpdates();
        lockNearbyView = false;
        checkGps();
        addNetworkBroadcastReceiver();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        wikidataEditListener.setAuthenticationStateListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (placesDisposable != null) {
            placesDisposable.dispose();
        }
        wikidataEditListener.setAuthenticationStateListener(null);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        snackbar = null;
        broadcastReceiver = null;
        wikidataEditListener.setAuthenticationStateListener(null);
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        // this means that this activity will not be recreated now, user is leaving it
        // or the activity is otherwise finishing
        if(getActivity().isFinishing()) {
            // we will not need this fragment anymore, this may also be a good place to signal
            // to the retained fragment object to perform its own cleanup.
            //removeMapFragment();
            removeListFragment();

        }
        if (broadcastReceiver != null) {
            getActivity().unregisterReceiver(broadcastReceiver);
            broadcastReceiver = null;
        }

        if (locationManager != null) {
            locationManager.removeLocationListener(this);
            locationManager.unregisterLocationManager();
        }
    }
}


