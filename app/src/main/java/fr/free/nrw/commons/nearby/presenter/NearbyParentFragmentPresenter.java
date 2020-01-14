package fr.free.nrw.commons.nearby.presenter;

import android.view.View;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.contract.NearbyMapContract;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import timber.log.Timber;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.CHECKED;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.UNCHECKED;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.UNKNOWN;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
                    WikidataEditListener.WikidataP18EditListener,
                    LocationUpdateListener,
                    NearbyParentFragmentContract.ViewsAreReadyCallback{


    private NearbyParentFragmentContract.View nearbyParentFragmentView;
    private NearbyMapContract.View nearbyMapFragmentView;
    private NearbyParentFragmentContract.NearbyListView nearbyListFragmentView;
    private boolean isNearbyLocked;
    private LatLng curLatLng;

    private boolean nearbyViewsAreReady;
    private boolean onTabSelected;
    private boolean mapInitialized;

    private Place placeToCenter;
    private boolean isPortraitMode;
    private boolean placesLoadedOnce;

    private LocationServiceManager locationServiceManager;

    public static NearbyParentFragmentPresenter presenterInstance;

    private NearbyParentFragmentPresenter(NearbyParentFragmentContract.NearbyListView nearbyListFragmentView,
                                         NearbyParentFragmentContract.View nearbyParentFragmentView,
                                         NearbyMapContract.View nearbyMapFragmentView,
                                         LocationServiceManager locationServiceManager) {
        this.nearbyListFragmentView = nearbyListFragmentView;
        this.nearbyParentFragmentView = nearbyParentFragmentView;
        this.nearbyMapFragmentView = nearbyMapFragmentView;
        this.nearbyMapFragmentView.viewsAreAssignedToPresenter(this);
        this.locationServiceManager = locationServiceManager;
    }

    // static method to create instance of Singleton class
    public static NearbyParentFragmentPresenter getInstance(NearbyParentFragmentContract.NearbyListView nearbyListFragmentView,
                                                            NearbyParentFragmentContract.View nearbyParentFragmentView,
                                                            NearbyMapContract.View nearbyMapFragmentView,
                                                            LocationServiceManager locationServiceManager) {
        if (presenterInstance == null) {
            presenterInstance = new NearbyParentFragmentPresenter(nearbyListFragmentView,
                    nearbyParentFragmentView,
                    nearbyMapFragmentView,
                    locationServiceManager);
        }
        return presenterInstance;
    }

    // We call this method when we are sure presenterInstance is not null
    public static NearbyParentFragmentPresenter getInstance() {
        return presenterInstance;
    }

    /**
     * Note: To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if nearby views are ready
     */
    @Override
    public void onTabSelected() {
        Timber.d("Nearby tab selected");
        onTabSelected = true;
        // The condition for initialize operations is both having views ready and tab is selected
        if (nearbyViewsAreReady && !mapInitialized) {
            checkForPermission();
        }
    }

    /**
     * -To initialize nearby operations both views should be ready and tab is selected.
     * Initializes nearby operations if tab selected, otherwise just sets nearby views are ready
     */
    @Override
    public void nearbyFragmentsAreReady() {
        Timber.d("Nearby fragments are ready to be used by presenter");
        nearbyViewsAreReady = true;
        // The condition for initialize operations is both having views ready and tab is selected
        if (onTabSelected) {
            checkForPermission();
        }
    }

    /**
     * Initializes nearby operations by following these steps:
     * -Checks for permission and perform if given
     */
    @Override
    public void checkForPermission() {
        Timber.d("checking for permission");
        nearbyParentFragmentView.checkPermissionsAndPerformAction(this::performNearbyOperationsIfPermissionGiven);
    }

    /**
     * - Adds search this area button action
     * - Adds camera move action listener
     * - Initializes nearby operations, registers listeners, broadcast receivers etc.
     */
    public void performNearbyOperationsIfPermissionGiven() {
        Timber.d("Permission is given, performing actions");
        this.nearbyParentFragmentView.addSearchThisAreaButtonAction();
        this.nearbyParentFragmentView.addOnCameraMoveListener(onCameraMove(getMapboxMap()));
        initializeMapOperations();
    }

    public void initializeMapOperations() {
        lockUnlockNearby(false);
        registerUnregisterLocationListener(false);
        nearbyParentFragmentView.addNetworkBroadcastReceiver();
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED, null);
        this.nearbyParentFragmentView.addSearchThisAreaButtonAction();
        this.nearbyMapFragmentView.addOnCameraMoveListener(onCameraMove(getMapboxMap()));
        nearbyParentFragmentView.setCheckBoxAction();
        mapInitialized = true;
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

    @Override
    public void backButtonClicked() {
        if(nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.listOptionMenuItemClicked();
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.setBottomSheetDetailsSmaller();
        } else {
            // Otherwise go back to contributions fragment
            nearbyParentFragmentView.setTabItemContributions();
        }
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
        if (isNearbyLocked) {
            nearbyParentFragmentView.disableFABRecenter();
        } else {
            nearbyParentFragmentView.enableFABRecenter();
        }
    }

    public void registerUnregisterLocationListener(boolean removeLocationListener) {
        if (removeLocationListener) {
            locationServiceManager.unregisterLocationManager();
            locationServiceManager.removeLocationListener(this);
            Timber.d("Location service manager unregistered and removed");
        } else {
            locationServiceManager.addLocationListener(this);
            nearbyParentFragmentView.registerLocationUpdates(locationServiceManager);
            Timber.d("Location service manager added and registered");
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
        Timber.d("Presenter updates map and list");
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }

        LatLng lastLocation = locationServiceManager.getLastLocation();
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
            Timber.d("LOCATION_SIGNIFICANTLY_CHANGED");
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            nearbyParentFragmentView.populatePlaces(lastLocation, lastLocation);

        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Timber.d("SEARCH_CUSTOM_AREA");
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            nearbyParentFragmentView.populatePlaces(lastLocation, cameraTarget);

        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly");
            if (!nearbyParentFragmentView.isSearchThisAreaButtonVisible()) { // Do not track users position if the user is checking around
                nearbyParentFragmentView.recenterMap(curLatLng);
            }
        }
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo, Marker selectedMarker) {
        nearbyMapFragmentView.updateMapMarkers(nearbyPlacesInfo.curLatLng, nearbyPlacesInfo.placeList, selectedMarker, this);
        nearbyMapFragmentView.addCurrentLocationMarker(nearbyPlacesInfo.curLatLng);
        nearbyMapFragmentView.updateMapToTrackPosition(nearbyPlacesInfo.curLatLng);
        lockUnlockNearby(false); // So that new location updates wont come
        nearbyParentFragmentView.setProgressBarVisibility(false);
        nearbyListFragmentView.updateListFragment(nearbyPlacesInfo.placeList);
        handleCenteringTaskIfAny();
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkersForCustomLocation(NearbyController.NearbyPlacesInfo nearbyPlacesInfo, Marker selectedMarker) {
        nearbyMapFragmentView.updateMapMarkers(nearbyPlacesInfo.curLatLng, nearbyPlacesInfo.placeList, selectedMarker, this);
        nearbyMapFragmentView.addCurrentLocationMarker(nearbyPlacesInfo.curLatLng);
        lockUnlockNearby(false); // So that new location updates wont come
        nearbyParentFragmentView.setProgressBarVisibility(false);
        nearbyListFragmentView.updateListFragment(nearbyPlacesInfo.placeList);
        handleCenteringTaskIfAny();
    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before
     * map is ready. So we will remember it when the map is ready
     */
    private void handleCenteringTaskIfAny() {
        if (!placesLoadedOnce) {
            placesLoadedOnce = true;
            nearbyMapFragmentView.centerMapToPlace(placeToCenter, isPortraitMode);
        }
    }

    @Override
    public void onWikidataEditSuccessful() {
        updateMapAndList(MAP_UPDATED, null);
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
        return () -> {
            // If our nearby markers are calculated at least once
            if (NearbyController.currentLocation != null) {
               double distance = mapboxMap.getCameraPosition().target.distanceTo
                        (LocationUtils.commonsLatLngToMapBoxLatLng(NearbyController.latestSearchLocation));
                if (nearbyParentFragmentView.isNetworkConnectionEstablished()) {
                    if (distance > NearbyController.latestSearchRadius) {
                        nearbyParentFragmentView.setSearchThisAreaButtonVisibility(true);
                    } else {
                        nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);
                    }
                }
            } else {
                nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);
            }
        };
    }

    @Override
    public void filterByMarkerType(List<Label> selectedLabels, int state, boolean filterForPlaceState, boolean filterForAllNoneType) {
        if (filterForAllNoneType) { // Means we will set labels based on states
            switch (state) {
                case UNKNOWN:
                    // Do nothing
                    break;
                case UNCHECKED:
                    nearbyMapFragmentView.filterOutAllMarkers();
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut();
                    break;
                case CHECKED:
                    nearbyMapFragmentView.displayAllMarkers();
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected();
                    break;
            }
        } else {
            nearbyMapFragmentView.filterMarkersByLabels(selectedLabels,
                    NearbyFilterState.getInstance().isExistsSelected(),
                    NearbyFilterState.getInstance().isNeedPhotoSelected(),
                    filterForPlaceState, filterForAllNoneType);
        }
    }

    @Override
    public void setCheckboxUnknown() {
        nearbyParentFragmentView.setCheckBoxState(CheckBoxTriStates.UNKNOWN);
    }

    @Override
    public void searchViewGainedFocus() {
        if(nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.hideBottomSheet();
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.hideBottomDetailsSheet();
        }
    }

    public View.OnClickListener onSearchThisAreaClicked() {
        return v -> {
            // Lock map operations during search this area operation
            nearbyParentFragmentView.setSearchThisAreaButtonVisibility(false);

            if (searchCloseToCurrentLocation()){
                updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED,
                        null);
            } else {
                updateMapAndList(SEARCH_CUSTOM_AREA,
                        getCameraTarget());
            }
        };
    }

    /**
     * Returns true if search this area button is used around our current location, so that
     * we can continue following our current location again
     * @return Returns true if search this area button is used around our current location
     */
    public boolean searchCloseToCurrentLocation() {
        double distance = LocationUtils.commonsLatLngToMapBoxLatLng(getCameraTarget())
                .distanceTo(new com.mapbox.mapboxsdk.geometry.LatLng(NearbyController.currentLocation.getLatitude()
                        , NearbyController.currentLocation.getLongitude()));
        if (distance > NearbyController.currentLocationSearchRadius * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Centers the map in nearby fragment to a given placeToCenter
     * @param place is new center of the map
     */
    public void centerMapToPlace(Place place, boolean isPortraitMode) {
        if (placesLoadedOnce) {
            nearbyMapFragmentView.centerMapToPlace(place, isPortraitMode);
        } else {
            this.isPortraitMode = isPortraitMode;
            this.placeToCenter = place;
        }
    }
}
