package fr.free.nrw.commons.nearby.mvp.presenter;

import javax.inject.Inject;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener {
    @Inject
    LocationServiceManager locationManager;

    private NearbyParentFragmentContract.View nearbyParentFragmentView;
    private boolean isNearbyLocked;

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

    /**
     * Nearby updates takes time, since they are network operations. During update time, we don't
     * want to get any other calls from user. So locking nearby.
     * @param isNearbyLocked true means lock, false means unlock
     */
    @Override
    public void lockNearby(boolean isNearbyLocked) {
        this.isNearbyLocked = isNearbyLocked;
        if (isNearbyLocked) {
            locationManager.unregisterLocationManager();
            locationManager.removeLocationListener(this);
        } else {
            nearbyParentFragmentView.registerLocationUpdates(locationManager);
            locationManager.addLocationListener(this);
        }
    }

    /**
     * This method should be the single point to update Map and List. Triggered by location
     * changes
     * @param locationChangeType defines if location changed significantly or slightly
     */
    @Override
    public void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType) {
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }



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
