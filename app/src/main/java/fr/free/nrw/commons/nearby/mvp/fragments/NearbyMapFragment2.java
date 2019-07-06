package fr.free.nrw.commons.nearby.mvp.fragments;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;
import com.mapbox.mapboxsdk.maps.MapboxMapOptions;
import com.mapbox.mapboxsdk.maps.SupportMapFragment;
import com.mapbox.mapboxsdk.utils.MapFragmentUtils;

import java.util.List;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import timber.log.Timber;

public class NearbyMapFragment2 extends SupportMapFragment implements NearbyMapContract.View {
    public NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback;

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Log.d("deneme2","on attach");
        ((NearbyParentFragment)getParentFragment()).childMapFragmentAttached();
    }

    /**
     * Creates a MapFragment instance
     *
     * @param mapboxMapOptions The configuration options to be used.
     * @return MapFragment created.
     */
    @NonNull
    public static NearbyMapFragment2 newInstance(@Nullable MapboxMapOptions mapboxMapOptions) {
        Log.d("deneme2","on NearbyMapFragment2 newInstance");
        NearbyMapFragment2 mapFragment = new NearbyMapFragment2();
        mapFragment.setArguments(MapFragmentUtils.createFragmentArgs(mapboxMapOptions));
        return mapFragment;
    }

    @Override
    public void showSearchThisAreaButton() {

    }

    @Override
    public void showInformationBottomSheet() {

    }

    @Override
    public void initViews() {

    }

    @Override
    public void updateMapMarkers(LatLng latLng, List<Place> placeList) {

    }

    @Override
    public void updateMapToTrackPosition(LatLng curLatLng) {

    }

    @Override
    public void setListeners() {

    }

    @Override
    public MapView setupMapView(Bundle savedInstanceState) {
        return null;
    }

    @Override
    public void addCurrentLocationMarker(LatLng curLatLng) {

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

    @Override
    public LatLng getCameraTarget() {
        return null;
    }

    @Override
    public MapboxMap getMapboxMap() {
        return null;
    }
    /**
     * Means that views are set in presenter to reference variables
     * @param viewsAreReadyCallback
     */
    @Override
    public void viewsAreAssignedToPresenter(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback) {
        Log.d("deneme2","on viewsAreAssignedToPresenter");

        Timber.d("Views are set");
        this.viewsAreReadyCallback = viewsAreReadyCallback;
        this.viewsAreReadyCallback.nearbyFragmentsAreReady();

    }

    @Override
    public void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener) {

    }

    @Override
    public void showPlaces() {

    }
}
