package fr.free.nrw.commons.nearby.mvp.presenter;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;

public class NearbyMapPresenter implements NearbyMapContract.UserActions {
    NearbyMapContract.View nearbyMapFragmentView;

    void NearbyMapPresenter(NearbyMapContract.View nearbyMapFragmentView) {
        this.nearbyMapFragmentView = nearbyMapFragmentView;
    }

    @Override
    public void searchThisArea() {

    }

    @Override
    public void storeSharedPrefs() {

    }

    @Override
    public void recenterMap() {

    }

    @Override
    public void updateMapMarkers(LatLng latLng) {

    }


    public void updateMapMarkers() {

    }

    @Override
    public void updateMapToTrackPosition() {

    }

    @Override
    public void getBundleContent() {

    }

    @Override
    public boolean addMapMovementListener() {
        return false;
    }

    @Override
    public void uploadImageGallery() {

    }

    @Override
    public void uploadImageCamera() {

    }

    @Override
    public void bookmarkItem() {

    }

    @Override
    public void getDirections() {

    }

    @Override
    public void seeWikidataItem() {

    }

    @Override
    public void seeWikipediaArticle() {

    }

    @Override
    public void seeCommonsFilePage() {

    }

    @Override
    public void rotateScreen() {

    }
}
