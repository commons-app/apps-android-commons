package fr.free.nrw.commons.nearby.mvp.fragments;

import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.annotations.Icon;
import com.mapbox.mapboxsdk.annotations.IconFactory;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.annotations.MarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback;
import com.mapbox.mapboxsdk.plugins.localization.LocalizationPlugin;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.utils.LocationUtils;
import timber.log.Timber;

public class NearbyMapFragment extends CommonsDaggerSupportFragment implements NearbyMapContract.View {
    @Inject
    @Named("default_preferences")
    JsonKvStore applicationKvStore;

    @Inject
    BookmarkLocationsDao bookmarkLocationDao;

    @BindView(R.id.bottom_sheet)
    View bottomSheetList;

    @BindView(R.id.bottom_sheet_details)
    View bottomSheetDetails;

    public MapView mapView;
    public MapboxMap mapboxMap;
    public NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

    private static final double ZOOM_LEVEL = 14f;

    // Variables for current location marker
    Icon blueIconOfCurLatLng;
    Marker currentLocationMarker;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("Nearby map fragment created");
        Mapbox.getInstance(getActivity(),
                getString(R.string.mapbox_commons_app_token));
        Mapbox.getTelemetry().setUserTelemetryRequestState(false);
        setRetainInstance(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Timber.d("onCreateView called");
        //View view = inflater.inflate(R.layout.fragment_nearby_map, container, false);
        setHasOptionsMenu(false);
        initViews();
        this.mapView = setupMapView(savedInstanceState);

        return mapView;
    }

    @Override
    public void initViews() {
        Timber.d("init views called");
        View view = ((NearbyParentFragment)getParentFragment()).view;
        ButterKnife.bind(this, view);
        bottomSheetListBehavior = BottomSheetBehavior.from(bottomSheetList);
        bottomSheetDetailsBehavior = BottomSheetBehavior.from(bottomSheetDetails);
        bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
        bottomSheetDetails.setVisibility(View.VISIBLE);

        fab_open = AnimationUtils.loadAnimation(getParentFragment().getActivity(), R.anim.fab_open);
        fab_close = AnimationUtils.loadAnimation(getParentFragment().getActivity(), R.anim.fab_close);
        rotate_forward = AnimationUtils.loadAnimation(getParentFragment().getActivity(), R.anim.rotate_forward);
        rotate_backward = AnimationUtils.loadAnimation(getParentFragment().getActivity(), R.anim.rotate_backward);

        blueIconOfCurLatLng = IconFactory.getInstance(getContext()).fromResource(R.drawable.current_location_marker);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        this.getView().setFocusableInTouchMode(true);
        this.getView().requestFocus();
        this.getView().setOnKeyListener((v, keyCode, event) -> {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_EXPANDED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);
                    return true;
                } else if (bottomSheetDetailsBehavior.getState() == BottomSheetBehavior
                        .STATE_COLLAPSED) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    mapView.getMapAsync(MapboxMap::deselectMarkers);
                    return true;
                }
            }
            return false;
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((NearbyParentFragment)getParentFragment()).childMapFragmentAttached();
    }

    @Override
    public MapView setupMapView(Bundle savedInstanceState) {
        Timber.d("setting up map view");
        boolean isDarkTheme = applicationKvStore.getBoolean("theme", false);
        /*MapboxMapOptions options = new MapboxMapOptions()
                .compassGravity(Gravity.BOTTOM | Gravity.LEFT)
                .compassMargins(new int[]{12, 0, 0, 24})
                .styleUrl(isDarkTheme ? Style.DARK : Style.OUTDOORS)
                .logoEnabled(false)
                .attributionEnabled(false)
                .camera(new CameraPosition.Builder()
                        .zoom(ZOOM_LEVEL)
                        .build());*/
/*
        MapView mapView = view.findViewById(R.id.mapView);
        mapView.onCreate(savedInstanceState);
        mapView.getMapAsync(new OnMapReadyCallback() {
                                @Override
                                public void onMapReady(MapboxMap mapboxMap) {
                                    Log.d("deneme2","nearby map is ready");
                                    NearbyMapFragment.this.mapboxMap = mapboxMap;
                                    viewsAreReadyCallback.nearbyMapViewReady();
                                }
                            }
        );

        return mapView;
*/


        /*if (!getParentFragment().getActivity().isFinishing()) {
            MapView mapView = new MapView(getParentFragment().getActivity(), options);
            // create map
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(mapboxMap -> {
                viewsAreReadyCallback.nearbyMapViewReady();
                /*LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Timber.d(exception.toString());
                }

                this.mapboxMap = mapboxMap;
                viewsAreReadyCallback.nearbyMapViewReady();
                //addMapMovementListeners();
                //updateMapSignificantlyForCurrentLocation();
            });*/
            //return mapView;
        //}
        return null;
    }

    @Override
    public void showSearchThisAreaButton() {

    }

    @Override
    public void showInformationBottomSheet() {

    }


    @Override
    public void setListeners() {

    }

    /**
     * Clears all existing map markers
     * @param curLatLng
     * @param placeList
     */
    @Override
    public void updateMapMarkers(LatLng curLatLng,  List<Place> placeList) {
        List<NearbyBaseMarker> customBaseMarkerOptions =  NearbyController
                .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng, // Curlatlang will be used to calculate distances
                        placeList,
                        getActivity(),
                        bookmarkLocationDao.getAllBookmarksLocations());
        // TODO: set search latlang here
        // TODO: arrange camera positions according to all other parameters

        // TODO: set position depening to botom sheet position heere
        mapboxMap.clear();

        addNearbyMarkersToMapBoxMap(customBaseMarkerOptions);
        // Re-enable mapbox gestures on custom location markers load
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
    }

    /**
     * Adds current location marker for given location and makes camera follow users new location
     * @param curLatLng given current location of user
     */
    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {

        Timber.d("Updates map current location marker to track user location");
        // Remove existing blue current location marker and add again for new location
        addCurrentLocationMarker(curLatLng);
        // Make camera target follow current position
        CameraPosition cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition), 1000);
    }

    /**
     * Adds a marker for the user's current position. Removes previous current location marker
     * if exists.
     */
    @Override
    public void addCurrentLocationMarker(LatLng curLatLng) {
        Timber.d("Adding current location marker");
        MarkerOptions currentLocationMarkerOptions = new MarkerOptions()
                .position(LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng));
        currentLocationMarkerOptions.setIcon(blueIconOfCurLatLng); // Set custom icon
        if (currentLocationMarker != null) { // Means that it is not our first current location
            // We should remove previously added current location marker first
            mapboxMap.removeMarker(currentLocationMarker);
            ValueAnimator markerAnimator = ObjectAnimator.ofObject(currentLocationMarker, "position",
                    new LatLngEvaluator(), currentLocationMarker.getPosition(),
                    LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng));
            markerAnimator.setDuration(1000);
            markerAnimator.start();
        }
        currentLocationMarker = mapboxMap.addMarker(currentLocationMarkerOptions);
    }

    /**
     * Adds markers for nearby places to mapbox map
     */
    public void addNearbyMarkersToMapBoxMap(@Nullable List<NearbyBaseMarker> baseMarkerList) {
        Timber.d("addNearbyMarkersToMapBoxMap is called");
        mapboxMap.addMarkers(baseMarkerList);
        mapboxMap.setOnInfoWindowCloseListener(marker -> {
            /*if (marker == selected) {
                bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
            }*/
        });
        mapView.getMapAsync(mapboxMap -> {
            mapboxMap.addMarkers(baseMarkerList);
            //fabRecenter.setVisibility(View.VISIBLE);
            mapboxMap.setOnInfoWindowCloseListener(marker -> {
                /*if (marker == selected) {
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                }*/
            });

            mapboxMap.setOnMarkerClickListener(marker -> {

                if (marker instanceof NearbyMarker) {
                    //this.selected = marker;
                    NearbyMarker nearbyMarker = (NearbyMarker) marker;
                    Place place = nearbyMarker.getNearbyBaseMarker().getPlace();
                    passInfoToSheet(place);
                    bottomSheetListBehavior.setState(BottomSheetBehavior.STATE_HIDDEN);
                    bottomSheetDetailsBehavior.setState(BottomSheetBehavior.STATE_COLLAPSED);

                }
                return false;
            });

        });
    }


    @Override
    public void setSearchThisAreaButtonVisibility(boolean visible) {

    }

    @Override
    public boolean isCurrentLocationMarkerVisible() {
        return false;
    }

    @Override
    public void prepareViewsForSheetPosition() {

    }

    @Override
    public void hideFABs() {

    }

    @Override
    public void showFABs() {

    }

    @Override
    public void addAnchorToBigFABs(FloatingActionButton floatingActionButton, int anchorID) {

    }

    @Override
    public void removeAnchorFromFABs(FloatingActionButton fab) {

    }

    @Override
    public void addAnchorToSmallFABs(FloatingActionButton floatingActionButton, int anchorID) {

    }

    @Override
    public void passInfoToSheet(Place place) {

    }

    @Override
    public void updateBookmarkButtonImage(Place place) {

    }

    @Override
    public void openWebView(Uri link) {

    }

    @Override
    public void animateFABs(boolean isFabOpen) {

    }

    @Override
    public void closeFabs(boolean isFabOpen) {

    }

    @Override
    public void updateMarker(boolean isBookmarked, Place place) {

    }

    /**
     * Means that views are set in presenter to reference variables
     * @param viewsAreReadyCallback
     */
    @Override
    public void viewsAreAssignedToPresenter(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback) {
        Timber.d("Views are set");
        this.viewsAreReadyCallback = viewsAreReadyCallback;
        this.viewsAreReadyCallback.nearbyFragmentsAreReady();

    }

    @Override
    public void showPlaces() {

    }

    /**
     * Returns camera target of current map view
     * @return camera target coordinate in terms of Commons LatLng
     */
    @Override
    public LatLng getCameraTarget() {
        return LocationUtils
                .mapBoxLatLngToCommonsLatLng(mapboxMap.getCameraPosition().target);
    }

    /**
     * Returns mapbox map current map view
     * @return mapbox map
     */
    @Override
    public MapboxMap getMapboxMap() {
        return mapboxMap;
    }

    @Override
    public void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener) {
        mapboxMap.addOnCameraMoveListener(onCameraMoveListener);
    }


    private static class LatLngEvaluator implements TypeEvaluator<com.mapbox.mapboxsdk.geometry.LatLng> {
        // Method is used to interpolate the marker animation.
        private com.mapbox.mapboxsdk.geometry.LatLng latLng = new com.mapbox.mapboxsdk.geometry.LatLng();

        @Override
        public com.mapbox.mapboxsdk.geometry.LatLng evaluate(float fraction, com.mapbox.mapboxsdk.geometry.LatLng startValue, com.mapbox.mapboxsdk.geometry.LatLng endValue) {
            latLng.setLatitude(startValue.getLatitude()
                    + ((endValue.getLatitude() - startValue.getLatitude()) * fraction));
            latLng.setLongitude(startValue.getLongitude()
                    + ((endValue.getLongitude() - startValue.getLongitude()) * fraction));
            return latLng;
        }
    }
}
