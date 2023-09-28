package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.utils.MapUtils.CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
import static fr.free.nrw.commons.utils.MapUtils.CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
import static fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.annotations.Polygon;
import com.mapbox.mapboxsdk.annotations.PolygonOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.pluginscalebar.ScaleBarOptions;
import com.mapbox.pluginscalebar.ScaleBarPlugin;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.MapStyle;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.ExploreMapRootFragment;
import fr.free.nrw.commons.explore.paging.LiveDataConverter;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.ExecutorUtils;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.utils.MapUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.UiUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class ExploreMapFragment extends CommonsDaggerSupportFragment
    implements ExploreMapContract.View, LocationUpdateListener {

    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private boolean isDarkTheme;
    private boolean isPermissionDenied;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation; // lask location of user
    private fr.free.nrw.commons.location.LatLng lastFocusLocation; // last location that map is focused
    public List<Media> mediaList;
    private boolean recenterToUserLocation; // true is recenter is needed (ie. when current location is in visible map boundaries)


    private MapboxMap.OnCameraMoveListener cameraMoveListener;
    private MapboxMap mapBox;
    private Place lastPlaceToCenter; // the last place that we centered the map
    private boolean isMapBoxReady;
    private Marker selectedMarker; // the marker that user selected
    private LatLngBounds projectorLatLngBounds; // current windows borders
    private Marker currentLocationMarker;
    private Polygon currentLocationPolygon;
    IntentFilter intentFilter = new IntentFilter(MapUtils.NETWORK_INTENT_ACTION);

    @Inject
    LiveDataConverter liveDataConverter;
    @Inject
    MediaClient mediaClient;
    @Inject
    LocationServiceManager locationManager;
    @Inject
    ExploreMapController exploreMapController;
    @Inject @Named("default_preferences")
    JsonKvStore applicationKvStore;
    @Inject
    BookmarkLocationsDao bookmarkLocationDao; // May be needed in future if we want to integrate bookmarking explore places
    @Inject
    SystemThemeUtils systemThemeUtils;

    private ExploreMapPresenter presenter;

    @BindView(R.id.map_view) MapView mapView;
    @BindView(R.id.bottom_sheet_details) View bottomSheetDetails;
    @BindView(R.id.map_progress_bar) ProgressBar progressBar;
    @BindView(R.id.fab_recenter) FloatingActionButton fabRecenter;
    @BindView(R.id.search_this_area_button) Button searchThisAreaButton;
    @BindView(R.id.tv_attribution) AppCompatTextView tvAttribution;

    @BindView(R.id.directionsButton) LinearLayout directionsButton;
    @BindView(R.id.commonsButton) LinearLayout commonsButton;
    @BindView(R.id.mediaDetailsButton) LinearLayout mediaDetailsButton;
    @BindView(R.id.description) TextView description;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.category) TextView distance;

    private ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), new ActivityResultCallback<Map<String, Boolean>>() {
        @Override
        public void onActivityResult(Map<String, Boolean> result) {
            boolean areAllGranted = true;
            for(final boolean b : result.values()) {
                areAllGranted = areAllGranted && b;
            }

            if (areAllGranted) {
                locationPermissionGranted();
            } else {
                if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                    DialogUtil.showAlertDialog(getActivity(), getActivity().getString(R.string.location_permission_title),
                        getActivity().getString(R.string.location_permission_rationale_nearby),
                        getActivity().getString(android.R.string.ok),
                        getActivity().getString(android.R.string.cancel),
                        () -> {
                            if (!(locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled())) {
                                showLocationOffDialog();
                            }
                        },
                        () -> isPermissionDenied = true,
                        null,
                        false);
                } else {
                    isPermissionDenied = true;
                }

            }
        }
    });


    @NonNull
    public static ExploreMapFragment newInstance() {
        ExploreMapFragment fragment = new ExploreMapFragment();
        fragment.setRetainInstance(true);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_explore_map, container, false);
        ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mapView.onStart();
        setSearchThisAreaButtonVisibility(false);
        tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
        initNetworkBroadCastReceiver();

        if (presenter == null) {
            presenter = new ExploreMapPresenter(bookmarkLocationDao);
        }
        setHasOptionsMenu(true);

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
            mapBoxMap.setStyle(isDarkTheme? MapStyle.DARK :
                MapStyle.OUTDOORS, style -> {
                final UiSettings uiSettings = mapBoxMap.getUiSettings();
                uiSettings.setCompassGravity(Gravity.BOTTOM | Gravity.LEFT);
                uiSettings.setCompassMargins(12, 0, 0, 24);
                uiSettings.setLogoEnabled(false);
                uiSettings.setAttributionEnabled(false);
                uiSettings.setRotateGesturesEnabled(false);
                isMapBoxReady = true;
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
            if (isResumed()) {
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
        presenter.onMapReady(exploreMapController);
    }

    private void registerNetworkReceiver() {
        if (getActivity() != null) {
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void performMapReadyActions() {
        if (isMapBoxReady) {
            if(!applicationKvStore.getBoolean("doNotAskForLocationPermission", false) ||
                PermissionUtils.hasPermission(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION})){
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
        setBottomSheetCallbacks();
    }

    /**
     * a) Creates bottom sheet behaviours from bottom sheet, sets initial states and visibility
     * b) Gets the touch event on the map to perform following actions:
     *      if bottom sheet details are expanded or collapsed hide the bottom sheet details.
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

    /**
     * Defines how bottom sheets will act on click
     */
    private void setBottomSheetCallbacks() {
        bottomSheetDetails.setOnClickListener(v -> {
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Timber.d("Location significantly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {
            handleLocationUpdate(latLng,LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    private boolean isUserBrowsing() {
        final boolean isUserBrowsing = lastKnownLocation!=null && !presenter.areLocationsClose(getCameraTarget(), lastKnownLocation);
        return isUserBrowsing;
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Timber.d("Location slightly changed");
        if (isMapBoxReady && latLng != null &&!isUserBrowsing()) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng,LOCATION_SLIGHTLY_CHANGED);
        }
    }

    private void handleLocationUpdate(final fr.free.nrw.commons.location.LatLng latLng, final LocationServiceManager.LocationChangeType locationChangeType){
        lastKnownLocation = latLng;
        exploreMapController.currentLocation = lastKnownLocation;
        presenter.updateMap(locationChangeType);
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {

    }

    @Override
    public boolean isNetworkConnectionEstablished() {
        return NetworkUtils.isInternetConnectionEstablished(getActivity());
    }

    @Override
    public void populatePlaces(LatLng curLatLng, LatLng searchLatLng) {
        final Observable<MapController.ExplorePlacesInfo> nearbyPlacesInfoObservable;
        if (curLatLng == null) {
            checkPermissionsAndPerformAction();
            return;
        }
        if (searchLatLng.equals(lastFocusLocation) || lastFocusLocation == null || recenterToUserLocation) { // Means we are checking around current location
            nearbyPlacesInfoObservable = presenter.loadAttractionsFromLocation(curLatLng, searchLatLng, true);
        } else {
            nearbyPlacesInfoObservable = presenter.loadAttractionsFromLocation(curLatLng, searchLatLng, false);
        }
        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(explorePlacesInfo -> {
                    updateMapMarkers(explorePlacesInfo, isCurrentLocationMarkerVisible());
                    mediaList = explorePlacesInfo.mediaList;
                    lastFocusLocation = searchLatLng;
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)+throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                }));
        if(recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    /**
     * Updates map markers according to latest situation
     * @param explorePlacesInfo holds several information as current location, marker list etc.
     */
    private void updateMapMarkers(final MapController.ExplorePlacesInfo explorePlacesInfo, final boolean shouldTrackPosition) {
        presenter.updateMapMarkers(explorePlacesInfo, selectedMarker,shouldTrackPosition);
    }

    private void showErrorMessage(final String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    @Override
    public void checkPermissionsAndPerformAction() {
        Timber.d("Checking permission and perfoming action");
        activityResultLauncher.launch(new String[]{permission.ACCESS_FINE_LOCATION});
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
                .zoom(ZOOM_LEVEL) // Same zoom level
                .build();
            mapBox.moveCamera(CameraUpdateFactory.newCameraPosition(position));
        }
        else if(locationManager.isGPSProviderEnabled() || locationManager.isNetworkProviderEnabled()){
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            setProgressBarVisibility(true);
        }
        else {
            Toast.makeText(getContext(), getString(R.string.nearby_location_not_available), Toast.LENGTH_LONG).show();
        }
        presenter.onMapReady(exploreMapController);
        registerUnregisterLocationListener(false);
        addOnCameraMoveListener();
    }

    public void registerUnregisterLocationListener(final boolean removeLocationListener) {
        MapUtils.registerUnregisterLocationListener(removeLocationListener, locationManager, this);
    }

    @Override
    public void recenterMap(LatLng curLatLng) {
        if (isPermissionDenied || curLatLng == null) {
            recenterToUserLocation = true;
            checkPermissionsAndPerformAction();
            if (!isPermissionDenied && !(locationManager.isNetworkProviderEnabled() || locationManager.isGPSProviderEnabled())) {
                showLocationOffDialog();
            }
            return;
        }
        addCurrentLocationMarker(curLatLng);
        final CameraPosition position;
        position = new CameraPosition.Builder()
            .target(new com.mapbox.mapboxsdk.geometry.LatLng(curLatLng.getLatitude(), curLatLng.getLongitude(), 0)) // Sets the new camera position
            .zoom(mapBox.getCameraPosition().zoom) // Same zoom level
            .build();

        mapBox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
    }

    @Override
    public void showLocationOffDialog() {
        // This creates a dialog box that prompts the user to enable location
        DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.ask_to_turn_location_on), getString(R.string.nearby_needs_location),
                getString(R.string.yes), getString(R.string.no),  this::openLocationSettings, null);
    }

    @Override
    public void openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = getActivity().getPackageManager();

        if (intent.resolveActivity(packageManager)!= null) {
            startActivity(intent);
            Toast.makeText(getContext(), R.string.recommend_high_accuracy_mode, Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(getContext(), R.string.cannot_open_location_settings, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    @Override
    public void displayBottomSheetWithInfo(final Marker marker) {
        selectedMarker = marker;
        final NearbyMarker nearbyMarker = (NearbyMarker) marker;
        final Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
        passInfoToSheet(place);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     * @param place Place of clicked nearby marker
     */
    private void passInfoToSheet(final Place place) {
        directionsButton.setOnClickListener(view -> Utils.handleGeoCoordinates(getActivity(),
            place.getLocation()));

        commonsButton.setVisibility(place.hasCommonsLink()?View.VISIBLE:View.GONE);
        commonsButton.setOnClickListener(view -> Utils.handleWebUrl(getContext(), place.siteLinks.getCommonsLink()));

        int index = 0;
        for (Media media : mediaList) {
            if (media.getFilename().equals(place.name)) {
                int finalIndex = index;
                mediaDetailsButton.setOnClickListener(view -> {
                    ((ExploreMapRootFragment) getParentFragment()).onMediaClicked(finalIndex);
                });
            }
            index ++;
        }
        title.setText(place.name.substring(5, place.name.lastIndexOf(".")));
        distance.setText(place.distance);
        // Remove label since it is double information
        String descriptionText = place.getLongDescription()
            .replace(place.getName() + " (","");
        descriptionText = (descriptionText.equals(place.getLongDescription()) ? descriptionText : descriptionText.replaceFirst(".$",""));
        // Set the short description after we remove place name from long description
        description.setText(descriptionText);
    }


    @Override
    public void addOnCameraMoveListener() {
        mapBox.addOnCameraMoveListener(cameraMoveListener);
    }

    @Override
    public void addSearchThisAreaButtonAction() {
        searchThisAreaButton.setOnClickListener(presenter.onSearchThisAreaClicked());
    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {
        if (isVisible) {
            searchThisAreaButton.setVisibility(View.VISIBLE);
        } else {
            searchThisAreaButton.setVisibility(View.GONE);
        }
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
        if (bottomSheetDetails.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isSearchThisAreaButtonVisible() {
        if (searchThisAreaButton.getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Removes old current location marker and adds a new one to display current location
     * @param curLatLng current location of user
     */
    @Override
    public void addCurrentLocationMarker(LatLng curLatLng) {
        if (null != curLatLng && !isPermissionDenied) {
            ExecutorUtils.get().submit(() -> {
                mapView.post(() -> removeCurrentLocationMarker());
                Timber.d("Adds current location marker");

                final Icon icon = IconFactory.getInstance(getContext())
                    .fromResource(R.drawable.current_location_marker);

                final MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                    .position(new com.mapbox.mapboxsdk.geometry.LatLng(curLatLng.getLatitude(),
                        curLatLng.getLongitude()));
                currentLocationMarkerOptions.setIcon(icon); // Set custom icon
                mapView.post(
                    () -> currentLocationMarker = mapBox.addMarker(currentLocationMarkerOptions));

                final List<com.mapbox.mapboxsdk.geometry.LatLng> circle = UiUtils
                    .createCircleArray(curLatLng.getLatitude(), curLatLng.getLongitude(),
                        curLatLng.getAccuracy() * 2, 100);

                final PolygonOptions currentLocationPolygonOptions = new PolygonOptions()
                    .addAll(circle)
                    .strokeColor(getResources().getColor(R.color.current_marker_stroke))
                    .fillColor(getResources().getColor(R.color.current_marker_fill));
                mapView.post(
                    () -> currentLocationPolygon = mapBox
                        .addPolygon(currentLocationPolygonOptions));
            });
        } else {
            Timber.d("not adding current location marker..current location is null");
        }
    }

    @Override
    public boolean isCurrentLocationMarkerVisible() {
        if (projectorLatLngBounds == null || currentLocationMarker == null) {
            Timber.d("Map projection bounds are null");
            return false;
        } else {
            Timber.d("Current location marker %s" , projectorLatLngBounds.contains(currentLocationMarker.getPosition()) ? "visible" : "invisible");
            return projectorLatLngBounds.contains(currentLocationMarker.getPosition());
        }
    }

    /**
     * Sets boundaries of visible region in terms of geolocation
     */
    @Override
    public void setProjectorLatLngBounds() {
        projectorLatLngBounds = mapBox.getProjection().getVisibleRegion().latLngBounds;
    }

    /**
     * Removes old current location marker
     */
    private void removeCurrentLocationMarker() {
        if (currentLocationMarker != null && mapBox!=null) {
            mapBox.removeMarker(currentLocationMarker);
            if (currentLocationPolygon != null) {
                mapBox.removePolygon(currentLocationPolygon);
            }
        }
    }

    /**
     * Update map camera to trac users current position
     * @param curLatLng
     */
    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {
        Timber.d("Updates map camera to track user position");
        final CameraPosition cameraPosition;
        if(isPermissionDenied){
            cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        }else{
            cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        }
        if(null!=mapBox) {
            mapBox.setCameraPosition(cameraPosition);
            mapBox.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition), 1000);
        }
    }

    @Override
    public LatLng getCameraTarget() {
        return mapBox == null ? null : LocationUtils.mapBoxLatLngToCommonsLatLng(mapBox.getCameraPosition().target);
    }

    /**
     * Centers map to a given place
     * @param place place to center
     */
    @Override
    public void centerMapToPlace(Place place) {
        MapUtils.centerMapToPlace(place, mapBox, lastPlaceToCenter, getActivity());
        Timber.d("Map is centered to place");
        final double cameraShift;
        if (null != place) {
            lastPlaceToCenter = place;
        }

        if (null != lastPlaceToCenter) {
            final Configuration configuration = getActivity().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT;
            } else {
                cameraShift = CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE;
            }
            final CameraPosition position = new CameraPosition.Builder()
                .target(LocationUtils.commonsLatLngToMapBoxLatLng(
                    new fr.free.nrw.commons.location.LatLng(
                        lastPlaceToCenter.location.getLatitude() - cameraShift,
                        lastPlaceToCenter.getLocation().getLongitude(),
                        0))) // Sets the new camera position
                .zoom(mapBox.getCameraPosition().zoom) // Same zoom level
                .build();
            mapBox.animateCamera(CameraUpdateFactory.newCameraPosition(position), 1000);
        }
    }

    @Override
    public LatLng getLastLocation() {
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastLocation();
        }
        return lastKnownLocation;
    }

    @Override
    public com.mapbox.mapboxsdk.geometry.LatLng getLastFocusLocation() {
        return lastFocusLocation == null? null : LocationUtils.commonsLatLngToMapBoxLatLng(lastFocusLocation);
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
    public void addNearbyMarkersToMapBoxMap(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker) {
        mapBox.clear();
        if (isMapBoxReady && mapBox != null) {
            mapBox.addMarkers(nearbyBaseMarkers);
            setMapMarkerActions(selectedMarker);
        }
    }

    private void setMapMarkerActions(final Marker selectedMarker) {
        if (mapBox != null) {
            mapBox.setOnInfoWindowCloseListener(marker -> {
                if (marker == selectedMarker) {
                    presenter.markerUnselected();
                }
            });

            mapBox.setOnMarkerClickListener(marker -> {
                if (marker instanceof NearbyMarker) {
                    presenter.markerSelected(marker);
                }
                return false;
            });
        }
    }

    @Override
    public void setMapBoundaries(CameraUpdate cameaUpdate) {
        mapBox.easeCamera(cameaUpdate);
    }

    @Override
    public void setFABRecenterAction(OnClickListener onClickListener) {
        fabRecenter.setOnClickListener(onClickListener);
    }

    @Override
    public boolean backButtonClicked() {
        if (!(bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_HIDDEN)) {
            bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            return true;
        } else {
            return false;
        }
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
                            snackbar = Snackbar.make(getView(), R.string.no_internet, Snackbar.LENGTH_INDEFINITE);
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
