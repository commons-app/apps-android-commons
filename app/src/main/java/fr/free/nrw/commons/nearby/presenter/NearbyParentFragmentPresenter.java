package fr.free.nrw.commons.nearby.presenter;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.CUSTOM_QUERY;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SLIGHTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.CHECKED;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.UNCHECKED;
import static fr.free.nrw.commons.nearby.CheckBoxTriStates.UNKNOWN;
import static fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT;

import android.location.Location;
import android.view.View;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.work.ExistingWorkPolicy;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.CheckBoxTriStates;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.NearbyController;
import fr.free.nrw.commons.nearby.NearbyFilterState;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.PlaceDao;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import fr.free.nrw.commons.utils.LocationUtils;
import fr.free.nrw.commons.wikidata.WikidataEditListener;
import io.reactivex.disposables.CompositeDisposable;
import java.lang.reflect.Proxy;
import java.util.List;
import timber.log.Timber;

public class NearbyParentFragmentPresenter
    implements NearbyParentFragmentContract.UserActions,
    WikidataEditListener.WikidataP18EditListener,
    LocationUpdateListener {

    private boolean isNearbyLocked;
    private LatLng currentLatLng;

    private boolean placesLoadedOnce;

    BookmarkLocationsDao bookmarkLocationDao;

    private @Nullable String customQuery;

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


    public NearbyParentFragmentPresenter(BookmarkLocationsDao bookmarkLocationDao) {
        this.bookmarkLocationDao = bookmarkLocationDao;
    }

    @Override
    public void attachView(NearbyParentFragmentContract.View view) {
        this.nearbyParentFragmentView = view;
    }

    @Override
    public void detachView() {
        this.nearbyParentFragmentView = DUMMY;
    }

    @Override
    public void removeNearbyPreferences(JsonKvStore applicationKvStore) {
        Timber.d("Remove place objects");
        applicationKvStore.remove(PLACE_OBJECT);
    }

    public void initializeMapOperations() {
        lockUnlockNearby(false);
        updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
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
            } else {
                nearbyParentFragmentView.animateFABs();
            }
        });

        nearbyParentFragmentView.setFABRecenterAction(v -> {
            nearbyParentFragmentView.recenterMap(currentLatLng);
        });

    }

    @Override
    public boolean backButtonClicked() {
        if (nearbyParentFragmentView.isAdvancedQueryFragmentVisible()) {
            nearbyParentFragmentView.showHideAdvancedQueryFragment(false);
            return true;
        } else if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
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

    /**
     * Nearby updates takes time, since they are network operations. During update time, we don't
     * want to get any other calls from user. So locking nearby.
     *
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
     * This method should be the single point to update Map and List. Triggered by location changes
     *
     * @param locationChangeType defines if location changed significantly or slightly
     */
    @Override
    public void updateMapAndList(LocationChangeType locationChangeType) {
        Timber.d("Presenter updates map and list");
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }

        LatLng lastLocation = nearbyParentFragmentView.getLastMapFocus();
        if (nearbyParentFragmentView.getMapCenter() != null) {
            currentLatLng = nearbyParentFragmentView.getMapCenter();
        } else {
            currentLatLng = lastLocation;
        }
        if (currentLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType.equals(CUSTOM_QUERY)) {
            Timber.d("ADVANCED_QUERY_SEARCH");
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            LatLng updatedLocationByUser = LocationUtils.deriveUpdatedLocationFromSearchQuery(
                customQuery);
            if (updatedLocationByUser == null) {
                updatedLocationByUser = lastLocation;
            }
            nearbyParentFragmentView.populatePlaces(updatedLocationByUser, customQuery);
        } else if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)
            || locationChangeType.equals(MAP_UPDATED)) {
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapCenter());
        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Timber.d("SEARCH_CUSTOM_AREA");
            lockUnlockNearby(true);
            nearbyParentFragmentView.setProgressBarVisibility(true);
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapFocus());
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly");
        }
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param nearbyPlaces This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkers(List<Place> nearbyPlaces, LatLng currentLatLng,
        boolean shouldTrackPosition) {
        if (null != nearbyParentFragmentView) {
            nearbyParentFragmentView.clearAllMarkers();
            List<BaseMarker> baseMarkers = NearbyController
                .loadAttractionsFromLocationToBaseMarkerOptions(currentLatLng,
                    // Curlatlang will be used to calculate distances
                    nearbyPlaces);
            nearbyParentFragmentView.updateMapMarkers(baseMarkers);
            lockUnlockNearby(false); // So that new location updates wont come
            nearbyParentFragmentView.setProgressBarVisibility(false);
            nearbyParentFragmentView.updateListFragment(nearbyPlaces);
        }
    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before map is
     * ready. So we will remember it when the map is ready
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
    public void filterByMarkerType(List<Label> selectedLabels, int state,
        boolean filterForPlaceState, boolean filterForAllNoneType) {
        if (filterForAllNoneType) {// Means we will set labels based on states
            switch (state) {
                case UNKNOWN:
                    // Do nothing
                    break;
                case UNCHECKED:
                    //TODO
                    nearbyParentFragmentView.filterOutAllMarkers();
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut();
                    break;
                case CHECKED:
                    // Despite showing all labels NearbyFilterState still should be applied
                    nearbyParentFragmentView.filterMarkersByLabels(selectedLabels,
                        filterForPlaceState, false);
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected();
                    break;
            }
        } else {
            nearbyParentFragmentView.filterMarkersByLabels(selectedLabels,
                filterForPlaceState, false);
        }
    }

    @Override
    @MainThread
    public void updateMapMarkersToController(List<BaseMarker> baseMarkers) {
        NearbyController.markerLabelList.clear();
        for (int i = 0; i < baseMarkers.size(); i++) {
            BaseMarker nearbyBaseMarker = baseMarkers.get(i);
            NearbyController.markerLabelList.add(
                new MarkerPlaceGroup(
                    bookmarkLocationDao.findBookmarkLocation(nearbyBaseMarker.getPlace()),
                    nearbyBaseMarker.getPlace()));
        }
    }

    @Override
    public void setCheckboxUnknown() {
        nearbyParentFragmentView.setCheckBoxState(CheckBoxTriStates.UNKNOWN);
    }

    @Override
    public void setAdvancedQuery(String query) {
        this.customQuery = query;
    }

    @Override
    public void searchViewGainedFocus() {
        if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.hideBottomSheet();
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.hideBottomDetailsSheet();
        }
    }

    /**
     * Initiates a search for places within the area. Depending on whether the search
     * is close to the current location, the map and list are updated
     * accordingly.
     */
    public void searchInTheArea(){
        if (searchCloseToCurrentLocation()) {
            updateMapAndList(LOCATION_SIGNIFICANTLY_CHANGED);
        } else {
            updateMapAndList(SEARCH_CUSTOM_AREA);
        }
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    public boolean searchCloseToCurrentLocation() {
        if (null == nearbyParentFragmentView.getLastMapFocus()) {
            return true;
        }
        //TODO
        Location mylocation = new Location("");
        Location dest_location = new Location("");
        dest_location.setLatitude(nearbyParentFragmentView.getMapFocus().getLatitude());
        dest_location.setLongitude(nearbyParentFragmentView.getMapFocus().getLongitude());
        mylocation.setLatitude(nearbyParentFragmentView.getLastMapFocus().getLatitude());
        mylocation.setLongitude(nearbyParentFragmentView.getLastMapFocus().getLongitude());
        Float distance = mylocation.distanceTo(dest_location);

        if (distance > 2000.0 * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }

    public void onMapReady() {
        if (null != nearbyParentFragmentView) {
            initializeMapOperations();
        }
    }
}
