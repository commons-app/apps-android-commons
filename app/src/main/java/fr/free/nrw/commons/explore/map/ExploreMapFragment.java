package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import com.google.android.material.snackbar.Snackbar;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.Style;
import com.mapbox.mapboxsdk.maps.UiSettings;
import com.mapbox.pluginscalebar.ScaleBarOptions;
import com.mapbox.pluginscalebar.ScaleBarPlugin;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.Utils;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.MainActivity;
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment;
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback;
import fr.free.nrw.commons.utils.NetworkUtils;
import fr.free.nrw.commons.utils.SystemThemeUtils;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class ExploreMapFragment extends CommonsDaggerSupportFragment
    implements ExploreMapContract.View, LocationUpdateListener {

    private static final float ZOOM_LEVEL = 14f;
    private static final float ZOOM_OUT = 0f;
    private BroadcastReceiver broadcastReceiver;
    private boolean isNetworkErrorOccurred;
    private Snackbar snackbar;
    private boolean isDarkTheme;
    private MapboxMap.OnCameraMoveListener cameraMoveListener;
    private MapboxMap mapBox;
    private boolean isMapBoxReady;
    private ExploreFragmentInstanceReadyCallback exploreFragmentInstanceReadyCallback;


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

    private View view;
    private ExploreMapPresenter presenter;


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
        presenter = new ExploreMapPresenter(bookmarkLocationDao);
        setHasOptionsMenu(true);
        // Inflate the layout for this fragment
        return view;
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        isDarkTheme = systemThemeUtils.isDeviceInNightMode();
        cameraMoveListener= () -> presenter.onCameraMove(mapBox.getCameraPosition().target);
        // presenter.attachView(this); TODO fix this
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(mapBoxMap -> {
            mapBox =mapBoxMap;
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
                    .zoom(ZOOM_OUT)
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
        /** // TODO nesli
        mapView.onResume();
        presenter.attachView(this);
        registerNetworkReceiver();
        if (isResumed() && ((MainActivity)getActivity()).activeFragment == ActiveFragment.NEARBY) {
            if(!isPermissionDenied && !applicationKvStore.getBoolean("doNotAskForLocationPermission", false)){
                startTheMap();
            }else{
                startMapWithoutPermission();
            }
        }
         **/
    }

    private void performMapReadyActions() {
        //TODO fill this Nesli
    }

    private void initViews() {
        Timber.d("init views called");
        initBottomSheets();
        setBottomSheetCallbacks();
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
        return false;
    }

    @Override
    public void populatePlaces(LatLng curlatLng) {

    }

    @Override
    public void checkPermissionsAndPerformAction() {

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

    }

    @Override
    public void addSearchThisAreaButtonAction() {

    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean isVisible) {

    }

    @Override
    public void setProgressBarVisibility(boolean isVisible) {

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

    }

    @Override
    public void updateMapMarkers(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker) {

    }

    @Override
    public LatLng getCameraTarget() {
        return null;
    }

    @Override
    public void centerMapToPlace(Place placeToCenter) {

    }

    @Override
    public LatLng getLastLocation() {
        return null;
    }

    @Override
    public com.mapbox.mapboxsdk.geometry.LatLng getLastFocusLocation() {
        return null;
    }

    @Override
    public boolean isCurrentLocationMarkerVisible() {
        return false;
    }

    @Override
    public void setProjectorLatLngBounds() {

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
                            presenter.updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
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
