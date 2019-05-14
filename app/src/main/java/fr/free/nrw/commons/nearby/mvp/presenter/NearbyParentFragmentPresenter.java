package fr.free.nrw.commons.nearby.mvp.presenter;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener {
    private NearbyParentFragmentContract.View nearbyParentFragmentView;

    public NearbyParentFragmentPresenter(NearbyParentFragmentContract.View nearbyParentFragmentView) {
        this.nearbyParentFragmentView = nearbyParentFragmentView;
    }

    @Override
    public void displayListFragmentExpanded() {

    }

    @Override
    public void onTabSelected() {
        initializeNearbyOperations();
    }

    @Override
    public void initializeNearbyOperations() {

    }

    @Override
    public void onWikidataEditSuccessful() {

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
}
