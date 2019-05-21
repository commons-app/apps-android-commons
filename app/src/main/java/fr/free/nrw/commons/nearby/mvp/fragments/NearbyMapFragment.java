package fr.free.nrw.commons.nearby.mvp.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.Mapbox;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import butterknife.BindView;
import butterknife.ButterKnife;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.nearby.NearbyFragment;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import timber.log.Timber;

public class NearbyMapFragment extends CommonsDaggerSupportFragment implements NearbyMapContract.View {
    @BindView(R.id.bottom_sheet)
    View bottomSheetList;

    @BindView(R.id.bottom_sheet_details)
    View bottomSheetDetails;

    MapView mapView;
    public NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback;

    private BottomSheetBehavior bottomSheetListBehavior;
    private BottomSheetBehavior bottomSheetDetailsBehavior;
    private Animation rotate_backward;
    private Animation fab_close;
    private Animation fab_open;
    private Animation rotate_forward;

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
        this.mapView = setupMapView(savedInstanceState);
        setHasOptionsMenu(false);
        initViews();
        return mapView;
    }

    @Override
    public void initViews() {
        Timber.d("init views called");
        View view = ((NearbyFragment)getParentFragment()).view;
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
        return null;
    }

    @Override
    public void showSearchThisAreaButton() {

    }

    @Override
    public void showInformationBottomSheet() {

    }

    @Override
    public void updateMapMarkers() {

    }

    @Override
    public void updateMapToTrackPosition() {

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
    public void addNearbyMarkersToMapBoxMap() {

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

    @Override
    public void setViewsAreReady(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback) {
        this.viewsAreReadyCallback = viewsAreReadyCallback;
    }

    @Override
    public void showPlaces() {

    }
}
