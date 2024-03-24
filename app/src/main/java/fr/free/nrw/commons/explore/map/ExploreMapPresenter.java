package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;


import android.location.Location;
import android.view.View;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.MapController.ExplorePlacesInfo;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.explore.map.ExploreMapController.NearbyBaseMarkerThumbCallback;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import io.reactivex.Observable;
import java.lang.reflect.Proxy;
import java.util.List;
import timber.log.Timber;

public class ExploreMapPresenter
    implements ExploreMapContract.UserActions,
    NearbyBaseMarkerThumbCallback {

    BookmarkLocationsDao bookmarkLocationDao;
    private boolean isNearbyLocked;
    private LatLng curLatLng;
    private ExploreMapController exploreMapController;

    private static final ExploreMapContract.View DUMMY = (ExploreMapContract.View) Proxy
        .newProxyInstance(
            ExploreMapContract.View.class.getClassLoader(),
            new Class[]{ExploreMapContract.View.class}, (proxy, method, args) -> {
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
    private ExploreMapContract.View exploreMapFragmentView = DUMMY;

    public ExploreMapPresenter(BookmarkLocationsDao bookmarkLocationDao) {
        this.bookmarkLocationDao = bookmarkLocationDao;
    }

    @Override
    public void updateMap(LocationChangeType locationChangeType) {
        Timber.d("Presenter updates map and list" + locationChangeType.toString());
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!exploreMapFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established");
            return;
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)) {
            Timber.d("LOCATION_SIGNIFICANTLY_CHANGED");
            lockUnlockNearby(true);
            exploreMapFragmentView.setProgressBarVisibility(true);
            exploreMapFragmentView.populatePlaces(exploreMapFragmentView.getMapCenter());
        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Timber.d("SEARCH_CUSTOM_AREA");
            lockUnlockNearby(true);
            exploreMapFragmentView.setProgressBarVisibility(true);
            exploreMapFragmentView.populatePlaces(exploreMapFragmentView.getMapFocus());
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly");
        }
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
            exploreMapFragmentView.disableFABRecenter();
        } else {
            exploreMapFragmentView.enableFABRecenter();
        }
    }

    @Override
    public void attachView(ExploreMapContract.View view) {
        exploreMapFragmentView = view;
    }

    @Override
    public void detachView() {
        exploreMapFragmentView = DUMMY;
    }

    /**
     * Sets click listener of FAB
     */
    @Override
    public void setActionListeners(JsonKvStore applicationKvStore) {
        exploreMapFragmentView.setFABRecenterAction(v -> {
            exploreMapFragmentView.recenterMap(curLatLng);
        });

    }

    @Override
    public boolean backButtonClicked() {
        return exploreMapFragmentView.backButtonClicked();
    }

    public void onMapReady(ExploreMapController exploreMapController) {
        this.exploreMapController = exploreMapController;
        exploreMapFragmentView.addSearchThisAreaButtonAction();
        if (null != exploreMapFragmentView) {
            exploreMapFragmentView.addSearchThisAreaButtonAction();
            initializeMapOperations();
        }
    }

    public void initializeMapOperations() {
        lockUnlockNearby(false);
        updateMap(LOCATION_SIGNIFICANTLY_CHANGED);
        exploreMapFragmentView.addSearchThisAreaButtonAction();
    }

    public Observable<ExplorePlacesInfo> loadAttractionsFromLocation(LatLng curLatLng,
        LatLng searchLatLng, boolean checkingAroundCurrent) {
        return Observable
            .fromCallable(() -> exploreMapController
                .loadAttractionsFromLocation(curLatLng, searchLatLng, checkingAroundCurrent));
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param explorePlacesInfo This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkers(
        MapController.ExplorePlacesInfo explorePlacesInfo) {
        if (explorePlacesInfo.mediaList != null) {
            prepareNearbyBaseMarkers(explorePlacesInfo);
        } else {
            lockUnlockNearby(false); // So that new location updates wont come
            exploreMapFragmentView.setProgressBarVisibility(false);
        }
    }

    void prepareNearbyBaseMarkers(MapController.ExplorePlacesInfo explorePlacesInfo) {
        exploreMapController
            .loadAttractionsFromLocationToBaseMarkerOptions(explorePlacesInfo.curLatLng,
                // Curlatlang will be used to calculate distances
                explorePlacesInfo.explorePlaceList,
                exploreMapFragmentView.getContext(),
                this,
                explorePlacesInfo);
    }

    @Override
    public void onNearbyBaseMarkerThumbsReady(List<BaseMarker> baseMarkers,
        ExplorePlacesInfo explorePlacesInfo) {
        if (null != exploreMapFragmentView) {
            exploreMapFragmentView.addMarkersToMap(baseMarkers);
            lockUnlockNearby(false); // So that new location updates wont come
            exploreMapFragmentView.setProgressBarVisibility(false);
        }
    }

    public View.OnClickListener onSearchThisAreaClicked() {
        return v -> {
            // Lock map operations during search this area operation
            exploreMapFragmentView.setSearchThisAreaButtonVisibility(false);

            if (searchCloseToCurrentLocation()) {
                updateMap(LOCATION_SIGNIFICANTLY_CHANGED);
            } else {
                updateMap(SEARCH_CUSTOM_AREA);
            }
        };
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    public boolean searchCloseToCurrentLocation() {
        if (null == exploreMapFragmentView.getLastMapFocus()) {
            return true;
        }

        Location mylocation = new Location("");
        Location dest_location = new Location("");
        dest_location.setLatitude(exploreMapFragmentView.getMapFocus().getLatitude());
        dest_location.setLongitude(exploreMapFragmentView.getMapFocus().getLongitude());
        mylocation.setLatitude(exploreMapFragmentView.getLastMapFocus().getLatitude());
        mylocation.setLongitude(exploreMapFragmentView.getLastMapFocus().getLongitude());
        Float distance = mylocation.distanceTo(dest_location);

        if (distance > 2000.0 * 3 / 4) {
            return false;
        } else {
            return true;
        }
    }

}
