package fr.free.nrw.commons.nearby.mvp.presenter;

import javax.inject.Inject;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.PERMISSION_JUST_GRANTED;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener {
    @Inject
    LocationServiceManager locationManager;

    private NearbyParentFragmentContract.View nearbyParentFragmentView;
    private boolean isNearbyLocked;
    private LatLng curLatLng;

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

        //nearbyParentFragmentView.registerLocationUpdates(locationManager);
        LatLng lastLocation = locationManager.getLastLocation();

        if (curLatLng != null) {
            // TODO figure out what is happening here about orientation change
        }

        curLatLng = lastLocation;

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
                || locationChangeType.equals(MAP_UPDATED)) {

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
