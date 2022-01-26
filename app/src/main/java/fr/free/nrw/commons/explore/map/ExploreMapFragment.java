package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.pluginscalebar.ScaleBarOptions;
import com.mapbox.pluginscalebar.ScaleBarPlugin;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.category.CategoryImagesCallback;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment;
import fr.free.nrw.commons.explore.paging.PagingContract.Presenter;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.MapUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class ExploreMapFragment extends PageableExploreMapRootFragment
    implements ExploreMapContract.View, LocationUpdateListener, CategoryImagesCallback {

    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private boolean isDarkTheme;
    private boolean isPermissionDenied;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation;
    private fr.free.nrw.commons.location.LatLng lastFocusLocation;
    private boolean recenterToUserLocation;


    private MapboxMap.OnCameraMoveListener cameraMoveListener;
    private MapboxMap mapBox;
    private Place lastPlaceToCenter;
    private boolean isMapBoxReady;
    private Marker selectedMarker;
    private ExploreFragmentInstanceReadyCallback exploreFragmentInstanceReadyCallback;
    IntentFilter intentFilter = new IntentFilter(MapUtils.NETWORK_INTENT_ACTION);


    @Inject
    LocationServiceManager locationManager;
    @Inject
    ExploreMapController exploreMapController;
    @Inject @Named("default_preferences")
    JsonKvStore applicationKvStore;
    @Inject
    BookmarkLocationsDao bookmarkLocationDao;
    @Inject
    SystemThemeUtils systemThemeUtils;

    @BindView(R.id.map_view)
    MapView mapView;
    @BindView(R.id.bottom_sheet_details)
    View bottomSheetDetails;
    @BindView(R.id.map_progress_bar)
    ProgressBar progressBar;
    @BindView(R.id.fab_recenter)
    FloatingActionButton fabRecenter;


    private View view;
    private ExploreMapPresenter presenter;
    private String customQuery;


    @NonNull
    public static ExploreMapFragment newInstance() {
        ExploreMapFragment fragment = new ExploreMapFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, final ViewGroup container,
        final Bundle savedInstanceState) {
        view = inflater.inflate(R.layout.fragment_explore_map, container, false);
        ButterKnife.bind(this, view);
        initNetworkBroadCastReceiver();
        if (presenter == null) {
            presenter = new ExploreMapPresenter(bookmarkLocationDao);
        }
        presenterReady(getContext());
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        isPermissionDenied = false;
        cameraMoveListener= () -> presenter.onCameraMove(mapBox.getCameraPosition().target);
        presenter.attachView(this);
        recenterToUserLocation = false;
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapBoxMap -> {
            mapBox = mapBoxMap;
            initViews();
            presenter.setActionListeners(applicationKvStore);
            mapBoxMap.setStyle(isDarkTheme? Style.DARK:Style.OUTDOORS, style -> {
                final UiSettings uiSettings = mapBoxMap.getUiSettings();
                uiSettings.setCompassGravity(Gravity.BOTTOM | Gravity.LEFT);
                uiSettings.setCompassMargins(12, 0, 0, 24);
                uiSettings.setLogoEnabled(false);
                uiSettings.setAttributionEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                isMapBoxReady = true;
                if(exploreFragmentInstanceReadyCallback!=null){
                    exploreFragmentInstanceReadyCallback.onReady();
                }
                performMapReadyActions();
                final CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(new com.mapbox.mapboxsdk.geometry.LatLng(51.50550, -0.07520))
                    .zoom(MapUtils.ZOOM_OUT)
                    .build();
                mapBoxMap.setCameraPosition(cameraPosition);

                final ScaleBarPlugin scaleBarPlugin = new ScaleBarPlugin(mapView, mapBoxMap);
                final int color = isDarkTheme ? R.color.bottom_bar_light : R.color.bottom_bar_dark;
                final ScaleBarOptions scaleBarOptions = new ScaleBarOptions(getContext())
                    .setTextColor(color)
                    .setTextSize(R.dimen.description_text_size)
                    .setBarHeight(R.dimen.tiny_gap)
                    .setBorderWidth(R.dimen.miniscule_margin)
                    .setMarginTop(R.dimen.tiny_padding)
                    .setMarginLeft(R.dimen.tiny_padding)
                    .setTextBarMargin(R.dimen.tiny_padding);
                scaleBarPlugin.create(scaleBarOptions);
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed() && ((MainActivity)getActivity()).activeFragment == ActiveFragment.EXPLORE) {
            if (!isPermissionDenied && !applicationKvStore
                .getBoolean("doNotAskForLocationPermission", false)) {
                startTheMap();
            } else {
                startMapWithoutPermission();
            }
        }
    }

    private void startTheMap() {
        mapView.onStart();
        performMapReadyActions();
    }

    private void startMapWithoutPermission() {
        mapView.onStart();

        applicationKvStore.putBoolean("doNotAskForLocationPermission", true);
        lastKnownLocation = MapUtils.defaultLatLng;
        MapUtils.centerMapToDefaultLatLng(mapBox);
        if (mapBox != null) {
            addOnCameraMoveListener();
        }
        presenter.onMapReady();
        // TODO nesli removeCurrentLocationMarker();
    }

    private void registerNetworkReceiver() {
        if (getActivity() != null) {
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void performMapReadyActions() {
        if (((MainActivity)getActivity()).activeFragment == ActiveFragment.EXPLORE && isMapBoxReady) {
            if(!applicationKvStore.getBoolean("doNotAskForLocationPermission", false) ||
                PermissionUtils.hasPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION)){
                checkPermissionsAndPerformAction();
            }else{
                isPermissionDenied = true;
                addOnCameraMoveListener();
            }
        }
    }

    private void initViews() {
        Timber.d("init views called");
        initBottomSheets();
        // TODO nesli setBottomSheetCallbacks();
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheet, sets initial states and visibility
     * b) Gets the touch event on the map to perform following actions:
     *      if bottom sheet details are expanded then collapse bottom sheet details.
     *      if bottom sheet details are collapsed then hide the bottom sheet details.
     */
    @SuppressLint("ClickableViewAccessibility")
    private void initBottomSheets() {
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);

        mapView.setOnTouchListener((v, event) -> {

            // Motion event is triggered two times on a touch event, one as ACTION_UP
            // and other as ACTION_DOWN, we only want one trigger per touch event.

            if(event.getAction() == MotionEvent.ACTION_DOWN) {
                if (bottomSheetDetailsBehavior.getState()
                    == BottomSheetBehavior.STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                } else if (bottomSheetDetailsBehavior.getState()
                    == BottomSheetBehavior.STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }
            }
            return false;
        });
    }

        @Override
    public void onLocationChangedSignificantly(LatLng latLng) {

    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {

    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {

    }

    @Override
    public boolean isNetworkConnectionEstablished() {
        return NetworkUtils.isInternetConnectionEstablished(getActivity());
    }

    @Override
    public void populatePlaces(LatLng curlatLng) {
        if (curlatLng.equals(lastFocusLocation) || lastFocusLocation == null || recenterToUserLocation) { // Means we are checking around current location
            populatePlacesForCurrentLocation(lastKnownLocation, curlatLng);
        } else {
            populatePlacesForAnotherLocation(lastKnownLocation, curlatLng);
        }
        if(recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    private void populatePlacesForCurrentLocation(final fr.free.nrw.commons.location.LatLng curlatLng,
        final fr.free.nrw.commons.location.LatLng searchLatLng){

        final Observable<ExploreMapController.NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> exploreMapController
                .loadAttractionsFromLocation(curlatLng, searchLatLng,
                    false, true, Utils.isMonumentsEnabled(new Date()), customQuery));

        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(nearbyPlacesInfo -> {
                    updateMapMarkers(nearbyPlacesInfo, true);
                    lastFocusLocation=searchLatLng;
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                }));
    }

    private void populatePlacesForAnotherLocation(final fr.free.nrw.commons.location.LatLng curlatLng,
        final fr.free.nrw.commons.location.LatLng searchLatLng){

        final Observable<ExploreMapController.NearbyPlacesInfo> nearbyPlacesInfoObservable = Observable
            .fromCallable(() -> exploreMapController
                .loadAttractionsFromLocation(curlatLng, searchLatLng,
                    false, true, Utils.isMonumentsEnabled(new Date()), customQuery));
        // TODO: check loadAttractionsromLocation with query parameter

        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(nearbyPlacesInfo -> {
                    updateMapMarkers(nearbyPlacesInfo, false);
                    lastFocusLocation = searchLatLng;
                },
                throwable -> {
                    Timber.e(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                }));
    }

    /**
     * Populates places for your location, should be used for finding nearby places around a
     * location where you are.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    private void updateMapMarkers(final ExploreMapController.NearbyPlacesInfo nearbyPlacesInfo, final boolean shouldUpdateSelectedMarker) {
        presenter.updateMapMarkers(nearbyPlacesInfo, selectedMarker,shouldUpdateSelectedMarker);
    }

    private void showErrorMessage(final String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    @Override
    public void checkPermissionsAndPerformAction() {
        Timber.d("Checking permission and perfoming action");
        PermissionUtils.checkPermissionsAndPerformAction(getActivity(),
            Manifest.permission.ACCESS_FINE_LOCATION,
            () -> locationPermissionGranted(),
            () -> isPermissionDenied = true,
            R.string.location_permission_title,
            R.string.location_permission_rationale_nearby);
    }

    private void locationPermissionGranted() {
        isPermissionDenied = false;

        applicationKvStore.putBoolean("doNotAskForLocationPermission", false);
        lastKnownLocation = locationManager.getLastLocation();
        fr.free.nrw.commons.location.LatLng target=lastFocusLocation;
        if(null == lastFocusLocation){
            target = lastKnownLocation;
        }
        if (lastKnownLocation != null) {
            final CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(target)) // Sets the new camera position
                .zoom(MapUtils.ZOOM_LEVEL) // Same zoom level
                .build();
            mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
        else if(locationManager.isGPSProviderEnabled()||locationManager.isNetworkProviderEnabled()){
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            setProgressBarVisibility(true);
        }
        else {
            Toast.makeText(getContext(), getString(R.string.nearby_location_not_available), Toast.LENGTH_LONG).show();
        }
        presenter.onMapReady();
        registerUnregisterLocationListener(false);
        addOnCameraMoveListener();
    }

    public void registerUnregisterLocationListener(final boolean removeLocationListener) {
        // TODO: do the same for nearby map
        MapUtils.registerUnregisterLocationListener(removeLocationListener, locationManager, this);
    }

    @Override
    public void recenterMap(LatLng curLatLng) {

    }

    @Override
    public void showLocationOffDialog() {

    }

    @Override
    public void openLocationSettings() {

    }

    @Override
    public void hideBottomDetailsSheet() {

    }

    @Override
    public void displayBottomSheetWithInfo(Marker marker) {

    }

    @Override
    public void addOnCameraMoveListener() {
        mapBox.addOnCameraMoveListener(cameraMoveListener);
    }

    @Override
    public void addSearchThisAreaButtonAction() {

    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {

    }

    @Override
    public void setProgressBarVisibility(boolean isVisible) {
        if (isVisible) {
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    public boolean isDetailsBottomSheetVisible() {
        return false;
    }

    @Override
    public void setBottomSheetDetailsSmaller() {

    }

    @Override
    public boolean isSearchThisAreaButtonVisible() {
        return false;
    }

    @Override
    public void addCurrentLocationMarker(LatLng curLatLng) {

    }

    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {
        Log.d("test","updateMapToTrackPosition");
    }

    @Override
    public void updateMapMarkers(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker) {
        Log.d("test","updateMapMarkers");
    }

    @Override
    public LatLng getCameraTarget() {
        return null;
    }

    @Override
    public void centerMapToPlace(Place placeToCenter) {
        MapUtils.centerMapToPlace(placeToCenter, mapBox, lastPlaceToCenter, getActivity());
    }

    @Override
    public LatLng getLastLocation() {
        return lastKnownLocation;
    }

    @Override
    public com.mapbox.mapboxsdk.geometry.LatLng getLastFocusLocation() {
        return lastFocusLocation == null? null : LocationUtils.commonsLatLngToMapBoxLatLng(lastFocusLocation);
    }
    @Override
    public boolean isCurrentLocationMarkerVisible() {
        return false;
    }

    @Override
    public void setProjectorLatLngBounds() {

    }

    @Override
    public void disableFABRecenter() {
        fabRecenter.setEnabled(false);
    }

    @Override
    public void enableFABRecenter() {
        fabRecenter.setEnabled(true);
    }

    @Override
    public void setCustomQuery(String customQuery) {
        this.customQuery = customQuery;
    }

    @NonNull
    @Override
    public Presenter<Media> getInjectedPresenter() {
        if (presenter != null) {
            return presenter;
        } else {
            presenter = new ExploreMapPresenter(bookmarkLocationDao);
            return presenter;
        }
    }

    @Override
    public Integer getContributionStateAt(int position) {
        return null;
    }

    @Override
    public void viewPagerNotifyDataSetChanged() {

    }

    @Override
    public void onMediaClicked(int position) {

    }

    public interface  ExploreFragmentInstanceReadyCallback{
        void onReady();
    }

    public void setExploreFragmentInstanceReadyCallback(
        ExploreFragmentInstanceReadyCallback exploreFragmentInstanceReadyCallback) {
        this.exploreFragmentInstanceReadyCallback = exploreFragmentInstanceReadyCallback;
    }

    /**
     * Adds network broadcast receiver to recognize connection established
     */
    private void initNetworkBroadCastReceiver() {
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(final Context context, final Intent intent) {
                if (getActivity() != null) {
                    if (NetworkUtils.isInternetConnectionEstablished(getActivity())) {
                        if (isNetworkErrorOccurred) {
                            presenter.updateMap(LOCATION_SIGNIFICANTLY_CHANGED);
                            isNetworkErrorOccurred = false;
                        }

                        if (snackbar != null) {
                            snackbar.dismiss();
                            snackbar = null;
                        }
                    } else {
                        if (snackbar == null) {
                            snackbar = Snackbar.make(view, R.string.no_internet, Snackbar.LENGTH_INDEFINITE);
                            setSearchThisAreaButtonVisibility(false);
                            setProgressBarVisibility(false);
                        }

                        isNetworkErrorOccurred = true;
                        snackbar.show();
                    }
                }
            }
        };
    }

}
