package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL;

import android.Manifest;
import android.Manifest.permission;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.content.ContextCompat;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.ExploreMapRootFragment;
import fr.free.nrw.commons.explore.paging.LiveDataConverter;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.DialogUtil;
import fr.free.nrw.commons.utils.MapUtils;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.PermissionUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Named;
import org.osmdroid.events.MapEventsReceiver;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.constants.GeoConstants;
import org.osmdroid.views.CustomZoomButtonsController;
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.MapEventsOverlay;
import org.osmdroid.views.overlay.Overlay;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.ScaleBarOverlay;
import org.osmdroid.views.overlay.ScaleDiskOverlay;
import org.osmdroid.views.overlay.TilesOverlay;
import timber.log.Timber;

public class ExploreMapFragment extends CommonsDaggerSupportFragment
    implements ExploreMapContract.View, LocationUpdateListener {

    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private boolean isDarkTheme;
    private boolean isPermissionDenied;
    private fr.free.nrw.commons.location.LatLng lastKnownLocation; // last location of user
    private fr.free.nrw.commons.location.LatLng lastFocusLocation; // last location that map is focused
    public List<Media> mediaList;
    private boolean recenterToUserLocation; // true is recenter is needed (ie. when current location is in visible map boundaries)
    private BaseMarker clickedMarker;
    private GeoPoint mapCenter;
    private GeoPoint lastMapFocus;
    IntentFilter intentFilter = new IntentFilter(MapUtils.NETWORK_INTENT_ACTION);

    @Inject
    LiveDataConverter liveDataConverter;
    @Inject
    MediaClient mediaClient;
    @Inject
    LocationServiceManager locationManager;
    @Inject
    ExploreMapController exploreMapController;
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;
    @Inject
    BookmarkLocationsDao bookmarkLocationDao; // May be needed in future if we want to integrate bookmarking explore places
    @Inject
    SystemThemeUtils systemThemeUtils;

    private ExploreMapPresenter presenter;

    public FragmentExploreMapBinding binding;

    private ActivityResultLauncher<String[]> activityResultLauncher = registerForActivityResult(
        new ActivityResultContracts.RequestMultiplePermissions(),
        new ActivityResultCallback<Map<String, Boolean>>() {
            @Override
            public void onActivityResult(Map<String, Boolean> result) {
                boolean areAllGranted = true;
                for (final boolean b : result.values()) {
                    areAllGranted = areAllGranted && b;
                }

                if (areAllGranted) {
                    locationPermissionGranted();
                } else {
                    if (shouldShowRequestPermissionRationale(permission.ACCESS_FINE_LOCATION)) {
                        DialogUtil.showAlertDialog(getActivity(),
                            getActivity().getString(R.string.location_permission_title),
                            getActivity().getString(R.string.location_permission_rationale_nearby),
                            getActivity().getString(android.R.string.ok),
                            getActivity().getString(android.R.string.cancel),
                            () -> {
                                if (!(locationManager.isNetworkProviderEnabled()
                                    || locationManager.isGPSProviderEnabled())) {
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
    public View onCreateView(
        @NonNull LayoutInflater inflater,
        ViewGroup container,
        Bundle savedInstanceState
    ) {
        binding = FragmentExploreMapBinding.inflate(getLayoutInflater());
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        setSearchThisAreaButtonVisibility(false);
        binding.tvAttribution.setText(Html.fromHtml(getString(R.string.map_attribution)));
        initNetworkBroadCastReceiver();

        if (presenter == null) {
            presenter = new ExploreMapPresenter(bookmarkLocationDao);
        }
        setHasOptionsMenu(true);

        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        isPermissionDenied = false;
        presenter.attachView(this);

        initViews();
        presenter.setActionListeners(applicationKvStore);

        org.osmdroid.config.Configuration.getInstance().load(this.getContext(),
            PreferenceManager.getDefaultSharedPreferences(this.getContext()));

        binding.mapView.setTileSource(TileSourceFactory.WIKIMEDIA);
        binding.mapView.setTilesScaledToDpi(true);

        org.osmdroid.config.Configuration.getInstance().getAdditionalHttpRequestProperties().put(
            "Referer", "http://maps.wikimedia.org/"
        );

        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(binding.mapView);
        scaleBarOverlay.setScaleBarOffset(15, 25);
        Paint barPaint = new Paint();
        barPaint.setARGB(200, 255, 250, 250);
        scaleBarOverlay.setBackgroundPaint(barPaint);
        scaleBarOverlay.enableScaleBar();
        binding.mapView.getOverlays().add(scaleBarOverlay);
        binding.mapView.getZoomController().setVisibility(CustomZoomButtonsController.Visibility.NEVER);
        binding.mapView.setMultiTouchControls(true);
        binding.mapView.getController().setZoom(ZOOM_LEVEL);
        performMapReadyActions();

        binding.mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (clickedMarker != null) {
                    removeMarker(clickedMarker);
                    addMarkerToMap(clickedMarker);
                    binding.mapView.invalidate();
                } else {
                    Timber.e("CLICKED MARKER IS NULL");
                }
                if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    // Back should first hide the bottom sheet if it is expanded
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else if (isDetailsBottomSheetVisible()) {
                    hideBottomDetailsSheet();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));

        binding.mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if (getLastMapFocus() != null) {
                    Location mylocation = new Location("");
                    Location dest_location = new Location("");
                    dest_location.setLatitude(binding.mapView.getMapCenter().getLatitude());
                    dest_location.setLongitude(binding.mapView.getMapCenter().getLongitude());
                    mylocation.setLatitude(getLastMapFocus().getLatitude());
                    mylocation.setLongitude(getLastMapFocus().getLongitude());
                    Float distance = mylocation.distanceTo(dest_location);//in meters
                    if (getLastMapFocus() != null) {
                        if (isNetworkConnectionEstablished() && (event.getX() > 0
                            || event.getY() > 0)) {
                            if (distance > 2000.0) {
                                setSearchThisAreaButtonVisibility(true);
                            } else {
                                setSearchThisAreaButtonVisibility(false);
                            }
                        }
                    } else {
                        setSearchThisAreaButtonVisibility(false);
                    }
                }

                return true;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                return false;
            }

        });

    }

    @Override
    public void onResume() {
        super.onResume();
        binding.mapView.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed()) {
            if (!isPermissionDenied && !applicationKvStore
                .getBoolean("doNotAskForLocationPermission", false)) {
                performMapReadyActions();
            } else {
                startMapWithoutPermission();
            }
        }
    }

    private void startMapWithoutPermission() {
        applicationKvStore.putBoolean("doNotAskForLocationPermission", true);
        lastKnownLocation = MapUtils.defaultLatLng;
        moveCameraToPosition(
            new GeoPoint(lastKnownLocation.getLatitude(), lastKnownLocation.getLongitude()));
        presenter.onMapReady(exploreMapController);
    }

    private void registerNetworkReceiver() {
        if (getActivity() != null) {
            getActivity().registerReceiver(broadcastReceiver, intentFilter);
        }
    }

    private void performMapReadyActions() {
        if (isDarkTheme) {
            binding.mapView.getOverlayManager().getTilesOverlay()
                .setColorFilter(TilesOverlay.INVERT_COLORS);
        }
        if (!applicationKvStore.getBoolean("doNotAskForLocationPermission", false) ||
            PermissionUtils.hasPermission(getActivity(),
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION})) {
            checkPermissionsAndPerformAction();
        } else {
            isPermissionDenied = true;
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
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(binding.bottomSheetDetailsBinding.getRoot());
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        binding.bottomSheetDetailsBinding.getRoot().setVisibility(View.VISIBLE);
    }

    /**
     * Defines how bottom sheets will act on click
     */
    private void setBottomSheetCallbacks() {
        binding.bottomSheetDetailsBinding.getRoot().setOnClickListener(v -> {
            if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_COLLAPSED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
            } else if (bottomSheetDetailsBehavior.getState()
                == BottomSheetBehavior.STATE_EXPANDED) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
            }
        });
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Timber.d("Location significantly changed");
        if (latLng != null) {
            handleLocationUpdate(latLng, LOCATION_SIGNIFICANTLY_CHANGED);
        }
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Timber.d("Location slightly changed");
        if (latLng != null) {//If the map has never ever shown the current location, lets do it know
            handleLocationUpdate(latLng, LOCATION_SLIGHTLY_CHANGED);
        }
    }

    private void handleLocationUpdate(final fr.free.nrw.commons.location.LatLng latLng,
        final LocationServiceManager.LocationChangeType locationChangeType) {
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
    public void populatePlaces(LatLng curLatLng) {
        final Observable<MapController.ExplorePlacesInfo> nearbyPlacesInfoObservable;
        if (curLatLng == null) {
            checkPermissionsAndPerformAction();
            return;
        }
        if (curLatLng.equals(getLastMapFocus())) { // Means we are checking around current location
            nearbyPlacesInfoObservable = presenter.loadAttractionsFromLocation(curLatLng,
                getLastMapFocus(), true);
        } else {
            nearbyPlacesInfoObservable = presenter.loadAttractionsFromLocation(getLastMapFocus(),
                curLatLng, false);
        }
        compositeDisposable.add(nearbyPlacesInfoObservable
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(explorePlacesInfo -> {
                    mediaList = explorePlacesInfo.mediaList;
                    if(mediaList == null) {
                        showResponseMessage(getString(R.string.no_pictures_in_this_area));
                    }
                    updateMapMarkers(explorePlacesInfo);
                    lastMapFocus = new GeoPoint(curLatLng.getLatitude(), curLatLng.getLongitude());
                },
                throwable -> {
                    Timber.d(throwable);
                    showErrorMessage(getString(R.string.error_fetching_nearby_places)
                    + throwable.getLocalizedMessage());
                    setProgressBarVisibility(false);
                    presenter.lockUnlockNearby(false);
                }));
        if (recenterToUserLocation) {
            recenterToUserLocation = false;
        }
    }

    /**
     * Updates map markers according to latest situation
     *
     * @param explorePlacesInfo holds several information as current location, marker list etc.
     */
    private void updateMapMarkers(final MapController.ExplorePlacesInfo explorePlacesInfo) {
        presenter.updateMapMarkers(explorePlacesInfo);
    }

    private void showErrorMessage(final String message) {
        ViewUtil.showLongToast(getActivity(), message);
    }

    private void showResponseMessage(final String message) {
        ViewUtil.showLongSnackbar(getView(), message);
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
        fr.free.nrw.commons.location.LatLng target = lastKnownLocation;
        if (lastKnownLocation != null) {
            GeoPoint targetP = new GeoPoint(target.getLatitude(), target.getLongitude());
            mapCenter = targetP;
            binding.mapView.getController().setCenter(targetP);
            recenterMarkerToPosition(targetP);
            moveCameraToPosition(targetP);
        } else if (locationManager.isGPSProviderEnabled()
            || locationManager.isNetworkProviderEnabled()) {
            locationManager.requestLocationUpdatesFromProvider(LocationManager.NETWORK_PROVIDER);
            locationManager.requestLocationUpdatesFromProvider(LocationManager.GPS_PROVIDER);
            setProgressBarVisibility(true);
        } else {
            Toast.makeText(getContext(), getString(R.string.nearby_location_not_available),
                Toast.LENGTH_LONG).show();
        }
        presenter.onMapReady(exploreMapController);
        registerUnregisterLocationListener(false);
    }

    public void registerUnregisterLocationListener(final boolean removeLocationListener) {
        MapUtils.registerUnregisterLocationListener(removeLocationListener, locationManager, this);
    }

    @Override
    public void recenterMap(LatLng curLatLng) {
        if (isPermissionDenied || curLatLng == null) {
            recenterToUserLocation = true;
            checkPermissionsAndPerformAction();
            if (!isPermissionDenied && !(locationManager.isNetworkProviderEnabled()
                || locationManager.isGPSProviderEnabled())) {
                showLocationOffDialog();
            }
            return;
        }
        recenterMarkerToPosition(new GeoPoint(curLatLng.getLatitude(), curLatLng.getLongitude()));
        binding.mapView.getController()
            .animateTo(new GeoPoint(curLatLng.getLatitude(), curLatLng.getLongitude()));
        if (lastMapFocus != null) {
            Location mylocation = new Location("");
            Location dest_location = new Location("");
            dest_location.setLatitude(binding.mapView.getMapCenter().getLatitude());
            dest_location.setLongitude(binding.mapView.getMapCenter().getLongitude());
            mylocation.setLatitude(lastMapFocus.getLatitude());
            mylocation.setLongitude(lastMapFocus.getLongitude());
            Float distance = mylocation.distanceTo(dest_location);//in meters
            if (lastMapFocus != null) {
                if (isNetworkConnectionEstablished()) {
                    if (distance > 2000.0) {
                        setSearchThisAreaButtonVisibility(true);
                    } else {
                        setSearchThisAreaButtonVisibility(false);
                    }
                }
            } else {
                setSearchThisAreaButtonVisibility(false);
            }
        }
    }

    @Override
    public void showLocationOffDialog() {
        // This creates a dialog box that prompts the user to enable location
        DialogUtil
            .showAlertDialog(getActivity(), getString(R.string.ask_to_turn_location_on),
                getString(R.string.nearby_needs_location),
                getString(R.string.yes), getString(R.string.no), this::openLocationSettings, null);
    }

    @Override
    public void openLocationSettings() {
        // This method opens the location settings of the device along with a followup toast.
        final Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        final PackageManager packageManager = getActivity().getPackageManager();

        if (intent.resolveActivity(packageManager) != null) {
            startActivity(intent);
            Toast.makeText(getContext(), R.string.recommend_high_accuracy_mode, Toast.LENGTH_LONG)
                .show();
        } else {
            Toast.makeText(getContext(), R.string.cannot_open_location_settings, Toast.LENGTH_LONG)
                .show();
        }
    }

    @Override
    public void hideBottomDetailsSheet() {
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
    }

    /**
     * Same bottom sheet carries information for all nearby places, so we need to pass information
     * (title, description, distance and links) to view on nearby marker click
     *
     * @param place Place of clicked nearby marker
     */
    private void passInfoToSheet(final Place place) {
        binding.bottomSheetDetailsBinding.directionsButton.setOnClickListener(view -> Utils.handleGeoCoordinates(getActivity(),
            place.getLocation()));

        binding.bottomSheetDetailsBinding.commonsButton.setVisibility(place.hasCommonsLink() ? View.VISIBLE : View.GONE);
        binding.bottomSheetDetailsBinding.commonsButton.setOnClickListener(
            view -> Utils.handleWebUrl(getContext(), place.siteLinks.getCommonsLink()));

        int index = 0;
        for (Media media : mediaList) {
            if (media.getFilename().equals(place.name)) {
                int finalIndex = index;
                binding.bottomSheetDetailsBinding.mediaDetailsButton.setOnClickListener(view -> {
                    ((ExploreMapRootFragment) getParentFragment()).onMediaClicked(finalIndex);
                });
            }
            index++;
        }
        binding.bottomSheetDetailsBinding.title.setText(place.name.substring(5, place.name.lastIndexOf(".")));
        binding.bottomSheetDetailsBinding.category.setText(place.distance);
        // Remove label since it is double information
        String descriptionText = place.getLongDescription()
            .replace(place.getName() + " (", "");
        descriptionText = (descriptionText.equals(place.getLongDescription()) ? descriptionText
            : descriptionText.replaceFirst(".$", ""));
        // Set the short description after we remove place name from long description
        binding.bottomSheetDetailsBinding.description.setText(descriptionText);
    }

    @Override
    public void addSearchThisAreaButtonAction() {
        binding.searchThisAreaButton.setOnClickListener(presenter.onSearchThisAreaClicked());
    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {
        binding.searchThisAreaButton.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void setProgressBarVisibility(boolean isVisible) {
        binding.mapProgressBar.setVisibility(isVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public boolean isDetailsBottomSheetVisible() {
        if (binding.bottomSheetDetailsBinding.getRoot().getVisibility() == View.VISIBLE) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean isSearchThisAreaButtonVisible() {
        return binding.bottomSheetDetailsBinding.getRoot().getVisibility() == View.VISIBLE;
    }

    @Override
    public LatLng getLastLocation() {
        if (lastKnownLocation == null) {
            lastKnownLocation = locationManager.getLastLocation();
        }
        return lastKnownLocation;
    }

    @Override
    public void disableFABRecenter() {
        binding.fabRecenter.setEnabled(false);
    }

    @Override
    public void enableFABRecenter() {
        binding.fabRecenter.setEnabled(true);
    }

    /**
     * Adds a markers to the map based on the list of NearbyBaseMarker.
     *
     * @param nearbyBaseMarkers The NearbyBaseMarker object representing the markers to be added.
     */
    @Override
    public void addMarkersToMap(List<BaseMarker> nearbyBaseMarkers) {
        clearAllMarkers();
        for (int i = 0; i < nearbyBaseMarkers.size(); i++) {
            addMarkerToMap(nearbyBaseMarkers.get(i));
        }
        binding.mapView.invalidate();
    }

    /**
     * Adds a marker to the map based on the specified NearbyBaseMarker.
     *
     * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be added.
     */
    private void addMarkerToMap(BaseMarker nearbyBaseMarker) {
        ArrayList<OverlayItem> items = new ArrayList<>();
        Bitmap icon = nearbyBaseMarker.getIcon();
        Drawable d = new BitmapDrawable(getResources(), icon);
        GeoPoint point = new GeoPoint(
            nearbyBaseMarker.getPlace().location.getLatitude(),
            nearbyBaseMarker.getPlace().location.getLongitude());
        OverlayItem item = new OverlayItem(nearbyBaseMarker.getPlace().name, null,
            point);
        item.setMarker(d);
        items.add(item);
        ItemizedOverlayWithFocus overlay = new ItemizedOverlayWithFocus(items,
            new OnItemGestureListener<OverlayItem>() {
                @Override
                public boolean onItemSingleTapUp(int index, OverlayItem item) {
                    final Place place = nearbyBaseMarker.getPlace();
                    if (clickedMarker != null) {
                        removeMarker(clickedMarker);
                        addMarkerToMap(clickedMarker);
                        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    }
                    clickedMarker = nearbyBaseMarker;
                    passInfoToSheet(place);
                    return true;
                }

                @Override
                public boolean onItemLongPress(int index, OverlayItem item) {
                    return false;
                }
            }, getContext());

        overlay.setFocusItemsOnTap(true);
        binding.mapView.getOverlays().add(overlay); // Add the overlay to the map
    }

    /**
     * Removes a marker from the map based on the specified NearbyBaseMarker.
     *
     * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be removed.
     */
    private void removeMarker(BaseMarker nearbyBaseMarker) {
        Place place = nearbyBaseMarker.getPlace();
        List<Overlay> overlays = binding.mapView.getOverlays();
        ItemizedOverlayWithFocus item;

        for (int i = 0; i < overlays.size(); i++) {
            if (overlays.get(i) instanceof ItemizedOverlayWithFocus) {
                item = (ItemizedOverlayWithFocus) overlays.get(i);
                OverlayItem overlayItem = item.getItem(0);

                if (place.location.getLatitude() == overlayItem.getPoint().getLatitude()
                    && place.location.getLongitude() == overlayItem.getPoint().getLongitude()) {
                    binding.mapView.getOverlays().remove(i);
                    binding.mapView.invalidate();
                    break;
                }
            }
        }
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    @Override
    public void clearAllMarkers() {
        binding.mapView.getOverlayManager().clear();
        GeoPoint geoPoint = mapCenter;
        if (geoPoint != null) {
            List<Overlay> overlays = binding.mapView.getOverlays();
            ScaleDiskOverlay diskOverlay =
                new ScaleDiskOverlay(this.getContext(),
                    geoPoint, 2000, GeoConstants.UnitOfMeasure.foot);
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.rgb(128, 128, 128));
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2f);
            diskOverlay.setCirclePaint2(circlePaint);
            Paint diskPaint = new Paint();
            diskPaint.setColor(Color.argb(40, 128, 128, 128));
            diskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            diskOverlay.setCirclePaint1(diskPaint);
            diskOverlay.setDisplaySizeMin(900);
            diskOverlay.setDisplaySizeMax(1700);
            binding.mapView.getOverlays().add(diskOverlay);
            org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(
                binding.mapView);
            startMarker.setPosition(geoPoint);
            startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(
                ContextCompat.getDrawable(this.getContext(), R.drawable.current_location_marker));
            startMarker.setTitle("Your Location");
            startMarker.setTextLabelFontSize(24);
            binding.mapView.getOverlays().add(startMarker);
        }
        ScaleBarOverlay scaleBarOverlay = new ScaleBarOverlay(binding.mapView);
        scaleBarOverlay.setScaleBarOffset(15, 25);
        Paint barPaint = new Paint();
        barPaint.setARGB(200, 255, 250, 250);
        scaleBarOverlay.setBackgroundPaint(barPaint);
        scaleBarOverlay.enableScaleBar();
        binding.mapView.getOverlays().add(scaleBarOverlay);
        binding.mapView.getOverlays().add(new MapEventsOverlay(new MapEventsReceiver() {
            @Override
            public boolean singleTapConfirmedHelper(GeoPoint p) {
                if (clickedMarker != null) {
                    removeMarker(clickedMarker);
                    addMarkerToMap(clickedMarker);
                    binding.mapView.invalidate();
                } else {
                    Timber.e("CLICKED MARKER IS NULL");
                }
                if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior.STATE_EXPANDED) {
                    // Back should first hide the bottom sheet if it is expanded
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                } else if (isDetailsBottomSheetVisible()) {
                    hideBottomDetailsSheet();
                }
                return true;
            }

            @Override
            public boolean longPressHelper(GeoPoint p) {
                return false;
            }
        }));
        binding.mapView.setMultiTouchControls(true);
    }

    /**
     * Recenters the map view to the specified GeoPoint and updates the marker to indicate the new
     * position.
     *
     * @param geoPoint The GeoPoint representing the new center position for the map.
     */
    private void recenterMarkerToPosition(GeoPoint geoPoint) {
        if (geoPoint != null) {
            binding.mapView.getController().setCenter(geoPoint);
            List<Overlay> overlays = binding.mapView.getOverlays();
            for (int i = 0; i < overlays.size(); i++) {
                if (overlays.get(i) instanceof org.osmdroid.views.overlay.Marker) {
                    binding.mapView.getOverlays().remove(i);
                } else if (overlays.get(i) instanceof ScaleDiskOverlay) {
                    binding.mapView.getOverlays().remove(i);
                }
            }
            ScaleDiskOverlay diskOverlay =
                new ScaleDiskOverlay(this.getContext(),
                    geoPoint, 2000, GeoConstants.UnitOfMeasure.foot);
            Paint circlePaint = new Paint();
            circlePaint.setColor(Color.rgb(128, 128, 128));
            circlePaint.setStyle(Paint.Style.STROKE);
            circlePaint.setStrokeWidth(2f);
            diskOverlay.setCirclePaint2(circlePaint);
            Paint diskPaint = new Paint();
            diskPaint.setColor(Color.argb(40, 128, 128, 128));
            diskPaint.setStyle(Paint.Style.FILL_AND_STROKE);
            diskOverlay.setCirclePaint1(diskPaint);
            diskOverlay.setDisplaySizeMin(900);
            diskOverlay.setDisplaySizeMax(1700);
            binding.mapView.getOverlays().add(diskOverlay);
            org.osmdroid.views.overlay.Marker startMarker = new org.osmdroid.views.overlay.Marker(
                binding.mapView);
            startMarker.setPosition(geoPoint);
            startMarker.setAnchor(org.osmdroid.views.overlay.Marker.ANCHOR_CENTER,
                org.osmdroid.views.overlay.Marker.ANCHOR_BOTTOM);
            startMarker.setIcon(
                ContextCompat.getDrawable(this.getContext(), R.drawable.current_location_marker));
            startMarker.setTitle("Your Location");
            startMarker.setTextLabelFontSize(24);
            binding.mapView.getOverlays().add(startMarker);
        }
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     */
    private void moveCameraToPosition(GeoPoint geoPoint) {
        binding.mapView.getController().animateTo(geoPoint);
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getLastMapFocus() {
        return lastMapFocus == null ? getMapCenter() : new fr.free.nrw.commons.location.LatLng(
            lastMapFocus.getLatitude(), lastMapFocus.getLongitude(), 100);
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getMapCenter() {
        fr.free.nrw.commons.location.LatLng latLnge = null;
        if (mapCenter != null) {
            latLnge = new fr.free.nrw.commons.location.LatLng(
                mapCenter.getLatitude(), mapCenter.getLongitude(), 100);
        }
        return latLnge;
    }

    @Override
    public fr.free.nrw.commons.location.LatLng getMapFocus() {
        fr.free.nrw.commons.location.LatLng mapFocusedLatLng = new fr.free.nrw.commons.location.LatLng(
            binding.mapView.getMapCenter().getLatitude(), binding.mapView.getMapCenter().getLongitude(), 100);
        return mapFocusedLatLng;
    }

    @Override
    public void setFABRecenterAction(OnClickListener onClickListener) {
        binding.fabRecenter.setOnClickListener(onClickListener);
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
                            snackbar = Snackbar.make(getView(), R.string.no_internet,
                                Snackbar.LENGTH_INDEFINITE);
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
