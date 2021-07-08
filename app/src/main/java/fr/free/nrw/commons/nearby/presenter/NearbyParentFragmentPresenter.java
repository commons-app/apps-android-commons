package fr.free.nrw.commons.nearby.presenter;

import android.view.View;

import androidx.annotation.MainThread;
import com.mapbox.mapboxsdk.annotations.Marker;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;

import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterState;
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
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

public class NearbyParentFragmentPresenter
        implements NearbyParentFragmentContract.UserActions,
        WikidataEditListener.WikidataP18EditListener,
        LocationUpdateListener {

    private boolean isNearbyLocked;
    private LatLng curLatLng;

    private boolean placesLoadedOnce;

    BookmarkLocationsDao bookmarkLocationDao;

    private static final NearbyParentFragmentContract.View DUMMY = (NearbyParentFragmentContract.View) Proxy.newProxyInstance(
            NearbyParentFragmentContract.View.class.getClassLoader(),
            new Class[]{NearbyParentFragmentContract.View.class}, (proxy, method, args) -> {
                if (method.getName().equals("onMyEvent")) {
                    return null;
                } else if (String.class == method.getReturnType()) {
                    return "";
                } else if (Integer.class == method.getReturnType()) {
                    return Integer.valueOf(0);
                } else if (int.class == method.getReturnType()) {
                    return 0;
                } else if (Boolean.class == method.getReturnType()) {
                    return Boolean.FALSE;
                } else if (boolean.class == method.getReturnType()) {
                    return false;
                } else {
                    return null;
                }
            }
    );
    private NearbyParentFragmentContract.View nearbyParentFragmentView = DUMMY;


    public NearbyParentFragmentPresenter(BookmarkLocationsDao bookmarkLocationDao){
        this.bookmarkLocationDao=bookmarkLocationDao;
    }

    @Override
    public void attachView(NearbyParentFragmentContract.View view){
        this.nearbyParentFragmentView=view;
    }

    @Override
    public void detachView(){
        this.nearbyParentFragmentView=DUMMY;
    }

    @Override
    public void removeNearbyPreferences(JsonKvStore applicationKvStore) {
        Timber.d("Remove place objects");
        applicationKvStore.remove(PLACE_OBJECT);
    }

    public void initializeMapOperations() {
        lockUnlockNearby(false);
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
        this.nearbyParentFragmentView.addSearchThisAreaButtonAction();
        nearbyParentFragmentView.setCheckBoxAction();
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
    public boolean backButtonClicked() {
        if(nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.listOptionMenuItemClicked();
            return true;
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.setBottomSheetDetailsSmaller();
            return true;
        }
        return false;
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

    /**
     * This method should be the single point to update Map and List. Triggered by location
     * changes
     * @param locationChangeType defines if location changed significantly or slightly
     */
    @Override
    public void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType) {
        Timber.d("Presenter updates map and list");
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }

        LatLng lastLocation = nearbyParentFragmentView.getLastLocation();
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
            nearbyParentFragmentView.populatePlaces(lastLocation);

        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Timber.d("SEARCH_CUSTOM_AREA");
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getCameraTarget());
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly");
            if (nearbyParentFragmentView.isCurrentLocationMarkerVisible()){ // Means user wants to see their live location
                nearbyParentFragmentView.recenterMap(curLatLng);
            }
        }
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param nearbyPlacesInfo This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkers(NearbyController.NearbyPlacesInfo nearbyPlacesInfo, Marker selectedMarker, boolean shouldTrackPosition) {
        if(null!=nearbyParentFragmentView) {
            List<NearbyBaseMarker> nearbyBaseMarkers = NearbyController
                    .loadAttractionsFromLocationToBaseMarkerOptions(nearbyPlacesInfo.curLatLng, // Curlatlang will be used to calculate distances
                            nearbyPlacesInfo.placeList,
                            nearbyParentFragmentView.getContext(),
                            bookmarkLocationDao.getAllBookmarksLocations());
            nearbyParentFragmentView.updateMapMarkers(nearbyBaseMarkers, selectedMarker);
            nearbyParentFragmentView.addCurrentLocationMarker(nearbyPlacesInfo.curLatLng);
            if(shouldTrackPosition){
                nearbyParentFragmentView.updateMapToTrackPosition(nearbyPlacesInfo.curLatLng);
            }
            lockUnlockNearby(false); // So that new location updates wont come
            nearbyParentFragmentView.setProgressBarVisibility(false);
            nearbyParentFragmentView.updateListFragment(nearbyPlacesInfo.placeList);
            handleCenteringTaskIfAny();
        }
    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before
     * map is ready. So we will remember it when the map is ready
     */
    private void handleCenteringTaskIfAny() {
        if (!placesLoadedOnce) {
            placesLoadedOnce = true;
            nearbyParentFragmentView.centerMapToPlace(null);
        }
    }

    @Override
    public void onWikidataEditSuccessful() {
        updateMapAndList(MAP_UPDATED);
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        Timber.d("Location significantly changed");
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {
        Timber.d("Location significantly changed");
        updateMapAndList(LOCATION_SLIGHTLY_CHANGED);
    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {
        Timber.d("Location changed medium");
    }

    @Override
    public void onCameraMove(com.mapbox.mapboxsdk.geometry.LatLng latLng) {
        nearbyParentFragmentView.setProjectorLatLngBounds();
            // If our nearby markers are calculated at least once
            if (NearbyController.latestSearchLocation != null) {
               double distance =latLng.distanceTo
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
    }

    @Override
    public void filterByMarkerType(List<Label> selectedLabels, int state, boolean filterForPlaceState, boolean filterForAllNoneType) {
        if (filterForAllNoneType) { // Means we will set labels based on states
            switch (state) {
                case UNKNOWN:
                    // Do nothing
                    break;
                case UNCHECKED:
                    nearbyParentFragmentView.filterOutAllMarkers();
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut();
                    break;
                case CHECKED:
                    // Despite showing all labels NearbyFilterState still should be applied
                    nearbyParentFragmentView.filterMarkersByLabels(selectedLabels,
                        NearbyFilterState.getInstance().isExistsSelected(),
                        NearbyFilterState.getInstance().isNeedPhotoSelected(),
                        NearbyFilterState.getInstance().isWlmSelected(),
                        filterForPlaceState, false);
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected();
                    break;
            }
        } else {
            nearbyParentFragmentView.filterMarkersByLabels(selectedLabels,
                    NearbyFilterState.getInstance().isExistsSelected(),
                    NearbyFilterState.getInstance().isNeedPhotoSelected(),
                    NearbyFilterState.getInstance().isWlmSelected(),
                    filterForPlaceState, false);
        }
    }

    @Override
    @MainThread
    public void updateMapMarkersToController(List<NearbyBaseMarker> nearbyBaseMarkers) {
        NearbyController.markerExistsMap = new HashMap<>();
        NearbyController.markerNeedPicMap = new HashMap<>();
        NearbyController.markerLabelList.clear();
        for (int i = 0; i < nearbyBaseMarkers.size(); i++) {
            NearbyBaseMarker nearbyBaseMarker = nearbyBaseMarkers.get(i);
            NearbyController.markerLabelList.add(
                    new MarkerPlaceGroup(nearbyBaseMarker.getMarker(), bookmarkLocationDao.findBookmarkLocation(nearbyBaseMarker.getPlace()), nearbyBaseMarker.getPlace()));
            //TODO: fix bookmark location
            NearbyController.markerExistsMap.put((nearbyBaseMarkers.get(i).getPlace().hasWikidataLink()), nearbyBaseMarkers.get(i).getMarker());
            NearbyController.markerNeedPicMap.put(((nearbyBaseMarkers.get(i).getPlace().pic == null) ? true : false), nearbyBaseMarkers.get(i).getMarker());
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
                updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
            } else {
                updateMapAndList(SEARCH_CUSTOM_AREA);
            }
        };
    }

    /**
     * Returns true if search this area button is used around our current location, so that
     * we can continue following our current location again
     * @return Returns true if search this area button is used around our current location
     */
    public boolean searchCloseToCurrentLocation() {
        if (null == nearbyParentFragmentView.getLastFocusLocation()) {
            return true;
        }
        double distance = LocationUtils.commonsLatLngToMapBoxLatLng(nearbyParentFragmentView.getCameraTarget())
                .distanceTo(nearbyParentFragmentView.getLastFocusLocation());
        if (distance > NearbyController.currentLocationSearchRadius * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }

    public void onMapReady() {
        if(null!=nearbyParentFragmentView) {
            nearbyParentFragmentView.addSearchThisAreaButtonAction();
            initializeMapOperations();
        }
    }

    public boolean areLocationsClose(LatLng cameraTarget, LatLng lastKnownLocation) {
        double distance = LocationUtils.commonsLatLngToMapBoxLatLng(cameraTarget)
                .distanceTo(LocationUtils.commonsLatLngToMapBoxLatLng(lastKnownLocation));
        if (distance > NearbyController.currentLocationSearchRadius * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }
}
