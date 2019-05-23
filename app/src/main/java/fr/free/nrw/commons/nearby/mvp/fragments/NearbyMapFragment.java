package fr.free.nrw.commons.nearby.mvp.fragments;

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
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.camera.CameraPosition;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.constants.Style;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
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
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.utils.LocationUtils;
import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
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
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
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
        viewsAreReadyCallback.nearbyFragmentAndMapViewReady();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        ((NearbyParentFragment)getParentFragment()).childMapFragmentAttached();
    }

    @Override
    public MapView setupMapView(Bundle savedInstanceState) {
        Log.d("deneme1","setupMapView");
        Timber.d("setupMapView called");
        boolean isDarkTheme = applicationKvStore.getBoolean("theme", false);
        MapboxMapOptions options = new MapboxMapOptions()
                .compassGravity(Gravity.BOTTOM | Gravity.LEFT)
                .compassMargins(new int[]{12, 0, 0, 24})
                .styleUrl(isDarkTheme ? Style.DARK : Style.OUTDOORS)
                .logoEnabled(false)
                .attributionEnabled(false)
                .camera(new CameraPosition.Builder()
                        .zoom(ZOOM_LEVEL)
                        .build());

        if (!getParentFragment().getActivity().isFinishing()) {
            MapView mapView = new MapView(getParentFragment().getActivity(), options);
            // create map
            mapView.onCreate(savedInstanceState);
            mapView.getMapAsync(mapboxMap -> {
                LocalizationPlugin localizationPlugin = new LocalizationPlugin(mapView, mapboxMap);

                try {
                    localizationPlugin.matchMapLanguageWithDeviceDefault();
                } catch (RuntimeException exception) {
                    Timber.d(exception.toString());
                }

                this.mapboxMap = mapboxMap;
                //addMapMovementListeners();
                //updateMapSignificantlyForCurrentLocation();
            });
            return mapView;
        }
        return null;
    }

    @Override
    public void showSearchThisAreaButton() {

    }

    @Override
    public void showInformationBottomSheet() {

    }

    @Override
    public void updateMapMarkers(LatLng curLatLng,  List<Place> placeList) {
        Log.d("deneme1","updateMapMarkers, curLatng:"+curLatLng);
        List<NearbyBaseMarker> customBaseMarkerOptions =  NearbyController
                .loadAttractionsFromLocationToBaseMarkerOptions(curLatLng, // Curlatlang will be used to calculate distances
                        placeList,
                        getActivity(),
                        bookmarkLocationDao.getAllBookmarksLocations());
        mapboxMap.clear();
        // TODO: set search latlang here
        CameraPosition cameraPosition = new CameraPosition.Builder().target
                (LocationUtils.commonsLatLngToMapBoxLatLng(curLatLng)).build();
        mapboxMap.setCameraPosition(cameraPosition);
        mapboxMap.animateCamera(CameraUpdateFactory
                .newCameraPosition(cameraPosition), 1000);
        // TODO: set position depening to botom sheet position heere
        // We are trying to find nearby places around our custom searched area, thus custom parameter is nonnull
        addNearbyMarkersToMapBoxMap(customBaseMarkerOptions);
        // Re-enable mapbox gestures on custom location markers load
        mapboxMap.getUiSettings().setAllGesturesEnabled(true);
    }

    @Override
    public void updateMapToTrackPosition() {
        //addCurrentLocationMarker(mapboxMap);

    }

    @Override
    public void setListeners() {

    }

    @Override
    public void addCurrentLocationMarker() {

    }

    @Override
    public void setSearchThisAreaButtonVisibility(boolean visible) {

    }

    @Override
    public boolean isCurrentLocationMarkerVisible() {
        return false;
    }

    @Override
    public void addNearbyMarkersToMapBoxMap(List<NearbyBaseMarker> baseMarkerOptions) {

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
     * Means that views are set in presenter
     * @param viewsAreReadyCallback
     */
    @Override
    public void viewsAreSet(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback) {
        Log.d("deneme1","viewsAreSet");
        this.viewsAreReadyCallback = viewsAreReadyCallback;

        this.viewsAreReadyCallback.nearbyFragmentAndMapViewReady();
    }

    @Override
    public void showPlaces() {

    }
}
