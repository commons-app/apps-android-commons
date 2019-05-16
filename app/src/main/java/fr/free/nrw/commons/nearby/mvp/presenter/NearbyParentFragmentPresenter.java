package fr.free.nrw.commons.nearby.mvp.presenter;

import javax.inject.Inject;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener {
    @Inject
    LocationServiceManager locationManager;

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

    /**
     * Initializes nearby operations by following these steps:
     * - Add this location listener to location manager
     */
    @Override
    public void initializeNearbyOperations() {
        locationManager.addLocationListener(this);
        nearbyParentFragmentView.registerLocationUpdates(locationManager);


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
