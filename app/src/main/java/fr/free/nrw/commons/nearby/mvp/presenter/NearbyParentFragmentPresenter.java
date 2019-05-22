package fr.free.nrw.commons.nearby.mvp.presenter;

import android.util.Log;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener,
                    NearbyParentFragmentContract.ViewsAreReadyCallback{


    private NearbyParentFragmentContract.View nearbyParentFragmentView;
    private NearbyMapContract.View nearbyMapFragmentView;
    private boolean isNearbyLocked;
    private LatLng curLatLng;

    boolean nearbyViewsAreReady;
    boolean onTabSelected;

    private LocationServiceManager locationServiceManager;

    public NearbyParentFragmentPresenter(NearbyParentFragmentContract.View nearbyParentFragmentView,
                                         NearbyMapContract.View nearbyMapFragmentView,
                                         LocationServiceManager locationServiceManager) {
        this.nearbyParentFragmentView = nearbyParentFragmentView;
        this.nearbyMapFragmentView = nearbyMapFragmentView;
        nearbyMapFragmentView.setViewsAreReady(this);
        this.locationServiceManager = locationServiceManager;
    }

    @Override
    public void displayListFragmentExpanded() {

    }

    /**
     * -To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if nearby views are ready
     */
    @Override
    public void onTabSelected() {
        Log.d("deneme1","onTabSelected");
        onTabSelected = true;
        if (nearbyViewsAreReady) {
            initializeNearbyOperations();
        }
    }

    /**
     * -To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if tab selected, otherwise just sets nearby views are ready
     */
    @Override
    public void nearbyFragmentAndMapViewReady() {
        Log.d("deneme1","nearbyFragmentAndMapViewReady");
        nearbyViewsAreReady = true;
        if (onTabSelected) {
            initializeNearbyOperations();
        }
    }

    /**
     * Initializes nearby operations by following these steps:
     * - Add this location listener to location manager
     */
    @Override
    public void initializeNearbyOperations() {
        Log.d("deneme1","initializeNearbyOperations");
        locationServiceManager.addLocationListener(this);
        nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
        Log.d("deneme1","initializeNearbyOperations2");

        // Nearby buttons should be active, they should be deactive only during update
        lockNearby(false);
        //This will start a consequence to check GPS depending on different API
        nearbyParentFragmentView.checkGps(locationServiceManager);
        //We will know when we went offline and online again
        nearbyParentFragmentView.addNetworkBroadcastReceiver();
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
            locationServiceManager.unregisterLocationManager();
            locationServiceManager.removeLocationListener(this);
        } else {
            nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
            locationServiceManager.addLocationListener(this);
        }
    }

    /**
     * This method should be the single point to update Map and List. Triggered by location
     * changes
     * @param locationChangeType defines if location changed significantly or slightly
     */
    @Override
    public void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType) {
        Log.d("deneme1","updateMapAndList");
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }

        //nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
        LatLng lastLocation = locationServiceManager.getLastLocation();
        Log.d("deneme1","locationServiceManager.getLastLocation():"+locationServiceManager.getLastLocation().toString());

        if (curLatLng != null) {
            // TODO figure out what is happening here about orientation change
        }

        curLatLng = lastLocation;

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
                || locationChangeType.equals(MAP_UPDATED)) {
            nearbyMapFragmentView.updateMapMarkers();
            nearbyMapFragmentView.updateMapToTrackPosition();
        } else {
            nearbyMapFragmentView.updateMapToTrackPosition();
        }
    }

    @Override
    public void onWikidataEditSuccessful() {

    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Log.d("deneme1","onLocationChangedSignificantly");

    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Log.d("deneme1","onLocationChangedSlightly");

    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        Log.d("deneme1","onLocationChangedMedium");
    }

}
