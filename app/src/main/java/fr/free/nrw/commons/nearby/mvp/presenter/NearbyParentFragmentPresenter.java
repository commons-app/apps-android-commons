package fr.free.nrw.commons.nearby.mvp.presenter;

import android.util.Log;
import android.view.View;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.mvp.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.utils.LocationUtils;

import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;

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
    boolean searchingThisArea;
    boolean nearbyMapViewReady;
    boolean nearbyOperationsInitialized;


    private LocationServiceManager locationServiceManager;

    public NearbyParentFragmentPresenter(NearbyParentFragmentContract.View nearbyParentFragmentView,
                                         NearbyMapContract.View nearbyMapFragmentView,
                                         LocationServiceManager locationServiceManager) {
        this.nearbyParentFragmentView = nearbyParentFragmentView;
        this.nearbyMapFragmentView = nearbyMapFragmentView;
        this.nearbyMapFragmentView.viewsAreAssignedToPresenter(this);
        this.locationServiceManager = locationServiceManager;
    }

    /**
     * Will be called on list button click to expand list fragment at 9/16 rate
     */
    @Override
    public void displayListFragmentExpanded() {

    }

    /**
     * Note: To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if nearby views are ready
     */
    @Override
    public void onTabSelected() {
        Timber.d("Nearby tab selected");
        Log.d("denemeTest","Nearby tab selected");
        onTabSelected = true;
        // The condition for initialize operations is both having views ready and tab is selected
        if (nearbyViewsAreReady) {
            initializeNearbyOperations();
        }
    }

    /**
     * -To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if tab selected, otherwise just sets nearby views are ready
     */
    @Override
    public void nearbyFragmentsAreReady() {
        Timber.d("Nearby fragments are ready to be used by presenter");
        Log.d("denemeTest","nearbyFragmentsAreReady");
        nearbyViewsAreReady = true;
        // The condition for initialize operations is both having views ready and tab is selected
        if (onTabSelected) {
            initializeNearbyOperations();
        }
    }

    /**
     * Initializes nearby operations by following these steps:
     * - Add this location listener to location manager
     * - Registers location updates with parent fragment, this methods also checks permissions
     * Note: Highly context dependent methods are handled in view and triggered from presenter
     */
    @Override
    public void initializeNearbyOperations() {
        Timber.d("initializing nearby operations started");
        // Add location listener to be notified about location changes
        //locationServiceManager.addLocationListener(this);
        nearbyParentFragmentView.checkPermissionsAndPerformAction(this::performNearbyOperationsIfPermissionGiven);

    }

    public void performNearbyOperationsIfPermissionGiven() {
        Timber.d("performNearbyOperationsIfPermissionGiven");
        //nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
        // Nearby buttons should be active, they should be inactive only during update
        // This will start a consequence to check GPS depending on different API
        //nearbyParentFragmentView.checkGps(locationServiceManager);
        // We will know when we went offline and online again
        //nearbyParentFragmentView.addNetworkBroadcastReceiver();
        //nearbyMapFragmentView.setupMapView(null);
        //nearbyOperationsInitialized();
        this.nearbyParentFragmentView.addSearchThisAreaButtonAction();
        this.nearbyParentFragmentView.addOnCameraMoveListener(onCameraMove(getMapboxMap()));
        initializeMapOperations();
    }


    /**
     * Will be called when map view is created and ready to be used.
     */
    @Override
    public void nearbyMapViewReady() {
        nearbyMapViewReady = true;
        if (nearbyOperationsInitialized) {
            initializeMapOperations();
        }
    }

    public void nearbyOperationsInitialized() {
        Log.d("deneme2","nearbyOperationsinitialized");
        nearbyOperationsInitialized = true;
        if (nearbyMapViewReady) {
            initializeMapOperations();
        }
    }

    public void initializeMapOperations() {
        Log.d("denemeTest","initializeMapOperations");
        nearbyParentFragmentView.initViewPositions();

        lockUnlockNearby(false);
        registerUnregisterLocationListener(false);
        nearbyParentFragmentView.addNetworkBroadcastReceiver();

        Timber.d("Nearby map view is created and ready");
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED, null);
        // TODO: document this prpoblem, if updateMapAndList is not called at checkGPS then this method never called, setup map view never ends
        this.nearbyParentFragmentView.addSearchThisAreaButtonAction();
        this.nearbyMapFragmentView.addOnCameraMoveListener(onCameraMove(getMapboxMap()));
    }

    /**
     * Sets click listeners of FABs, and 2 bottom sheets
     */
    @Override
    public void setActionListeners(JsonKvStore applicationKvStore) {
        nearbyParentFragmentView.setFABPlusAction(v -> {
            if (applicationKvStore.getBoolean("login_skipped", false)) {
                // prompt the user to login
                nearbyParentFragmentView.displayLoginSkippedWarning();
            }else {
                nearbyParentFragmentView.animateFABs();
            }
        });

        nearbyParentFragmentView.setFABRecenterAction(v -> {
             nearbyParentFragmentView.recenterMap(curLatLng);
        });

    }

    public void markerUnselected() {
        nearbyParentFragmentView.hideBottomSheet();
    }


    public void markerSelected(Marker marker) {
        nearbyParentFragmentView.displayBottomSheetWithInfo(marker);
    }


    /**
     * Nearby updates takes time, since they are network operations. During update time, we don't
     * want to get any other calls from user. So locking nearby.
     * @param isNearbyLocked true means lock, false means unlock
     */
    @Override
    public void lockUnlockNearby(boolean isNearbyLocked) {
        this.isNearbyLocked = isNearbyLocked;
    }

    public void registerUnregisterLocationListener(boolean removeLocationListener) {
        if (removeLocationListener) {
            locationServiceManager.unregisterLocationManager();
            locationServiceManager.removeLocationListener(this);
            Timber.d("Nearby locked");
        } else {
            locationServiceManager.addLocationListener(this);
            nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
            Timber.d("Nearby unlocked");
        }
    }

    public LatLng getCameraTarget() {
        return nearbyMapFragmentView.getCameraTarget();
    }
    public MapboxMap getMapboxMap() {
        return nearbyMapFragmentView.getMapboxMap();
    }

    /**
     * This method should be the single point to update Map and List. Triggered by location
     * changes
     * @param locationChangeType defines if location changed significantly or slightly
     * @param cameraTarget will be used for search this area mode, when searching around
     *                    user's camera target
     */
    @Override
    public void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType, LatLng cameraTarget) {
        Log.d("denemeTest","updateMapAndList");
        if (isNearbyLocked) {
            Log.d("denemeTest","isNearbyLocked");
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            Log.d("denemeTest","!nearbyParentFragmentView.isNetworkConnectionEstablished()");
            return;
        }

        LatLng lastLocation = locationServiceManager.getLastLocation();

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
            Log.d("denemeTest","1");
            lockUnlockNearby(true);
            nearbyParentFragmentView.populatePlaces(lastLocation, lastLocation);
            nearbyParentFragmentView.setSearchThisAreaProgressVisibility(false);
            //nearbyMapFragmentView.updateMapToTrackPosition(curLatLng);
            // TODO: when unneeded populate places call problem is solved, open commented out line and remove it from update map markers method
            // TODO dont forget map updated state after an wikidata item is updated

        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Log.d("denemeTest","2");
            lockUnlockNearby(true);
            nearbyParentFragmentView.populatePlaces(lastLocation, cameraTarget);
            nearbyParentFragmentView.setSearchThisAreaProgressVisibility(false);
            searchingThisArea = false;
        } else { // Means location changed slightly, ie user is walking or driving.
            Log.d("denemeTest","3");
            nearbyMapFragmentView.updateMapToTrackPosition(curLatLng);
            searchingThisArea = false;
        }

        // TODO: update camera angle accordingly here, 1- search this area mode, 2- following current location, 3- list sheet expanded, 4- landcaped
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    public void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo, Marker selectedMarker) {
        nearbyMapFragmentView.updateMapMarkers(nearbyPlacesInfo.curLatLng, nearbyPlacesInfo.placeList, selectedMarker, this);
        nearbyMapFragmentView.addCurrentLocationMarker(nearbyPlacesInfo.curLatLng);
        nearbyMapFragmentView.updateMapToTrackPosition(nearbyPlacesInfo.curLatLng);
        lockUnlockNearby(false); // So that new location updates wont come
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has place list information and distances.
     */
    public void updateMapMarkersForCustomLocation(NearbyController.NearbyPlacesInfo nearbyPlacesInfo, Marker selectedMarker) {
        nearbyMapFragmentView.updateMapMarkers(nearbyPlacesInfo.curLatLng, nearbyPlacesInfo.placeList, selectedMarker, this);
        nearbyMapFragmentView.addCurrentLocationMarker(nearbyPlacesInfo.curLatLng);
        lockUnlockNearby(false); // So that new location updates wont come
    }

    @Override
    public void onWikidataEditSuccessful() {

    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Timber.d("Location significantly changed");
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED, null);

    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Timber.d("Location significantly changed");
        updateMapAndList(LOCATION_SLIGHTLY_CHANGED, null);
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        Timber.d("Location changed medium");
    }

    @Override
    public MapboxMap.OnCameraMoveListener onCameraMove(MapboxMap mapboxMap) {
        Log.d("denemeTestt","cameramoving1");

        return () -> {
            Log.d("denemeTestt","cameramoving2");

            // If our nearby markers are calculated at least once
            if (NearbyController.currentLocation != null) {
                Log.d("denemeTestt","NearbyController.currentLocation != null");
                double distance = mapboxMap.getCameraPosition().target.distanceTo
                        (LocationUtils.commonsLatLngToMapBoxLatLng(NearbyController.latestSearchLocation));
                if (nearbyParentFragmentView.isNetworkConnectionEstablished()) {
                    if (distance > NearbyController.latestSearchRadius) {
                        Log.d("denemeTestt","distance > NearbyController.latestSearchRadius");
                        nearbyParentFragmentView.setSearchThisAreaButtonVisibility(true);
                    } else {
                        Log.d("denemeTestt","distance < NearbyController.latestSearchRadius");
                        nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);
                    }
                }
            } else {
                nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);
            }
        };
    }

    public View.OnClickListener onSearchThisAreaClicked() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("denemeTestt","onSearchThisAreaClicked");
                // Lock map operations during search this area operation
                // TODO: test lock nearby
                nearbyParentFragmentView.setSearchThisAreaProgressVisibility(true);
                // TODO: make this invisible at somewhere
                nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);

                if (searchCloseToCurrentLocation()){
                    Log.d("denemeTestt","searchCloseToCurrentLocation()");
                    updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED,
                            null);
                } else {
                    Log.d("denemeTestt","!searchCloseToCurrentLocation()+"+getCameraTarget());
                    updateMapAndList(SEARCH_CUSTOM_AREA,
                            getCameraTarget());
                }
            }
        };
    }

    /**
     * Returns true if search this area button is used around our current location, so that
     * we can continue following our current location again
     * @return Returns true if search this area button is used around our current location
     */
    public boolean searchCloseToCurrentLocation() {
        Log.d("denemeTestt","searchCloseToCurrentLocation method");
        double distance = LocationUtils.commonsLatLngToMapBoxLatLng(getCameraTarget())
                .distanceTo(new com.mapbox.mapboxsdk.geometry.LatLng(NearbyController.currentLocation.getLatitude()
                        , NearbyController.currentLocation.getLongitude()));
        if (distance > NearbyController.currentLocationSearchRadius * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }
}
