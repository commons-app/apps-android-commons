package fr.free.nrw.commons.nearby.mvp.fragments;

import android.net.Uri;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;

public class NearbyMapFragment extends CommonsDaggerSupportFragment implements NearbyMapContract.View {
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
    public void updateMapMarkers() {

    }

    @Override
    public void updateMapToTrackPosition() {

    }

    @Override
    public void setListeners() {

    }

    @Override
    public void setupMapView() {

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
    public void showPlaces() {

    }
}
