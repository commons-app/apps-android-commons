package fr.free.nrw.commons.explore.map;

import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.MAP_UPDATED;
import static fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType.SEARCH_CUSTOM_AREA;


import android.util.Log;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory;
import com.mapbox.mapboxsdk.geometry.LatLngBounds;
import fr.free.nrw.commons.MapController;
import fr.free.nrw.commons.MapController.ExplorePlacesInfo;
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.utils.LocationUtils;
import io.reactivex.Observable;
import java.lang.reflect.Proxy;
import java.util.List;
import timber.log.Timber;

public class ExploreMapPresenter
    implements ExploreMapContract.UserActions,
    LocationUpdateListener{
    BookmarkLocationsDao bookmarkLocationDao;
    private boolean isNearbyLocked;
    private boolean placesLoadedOnce;
    private LatLng curLatLng;
    private String query;
    private boolean isFromSearchActivity;
    private ExploreMapController exploreMapController;
    private float ZOOM_LEVEL = 14f;



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

    public ExploreMapPresenter(BookmarkLocationsDao bookmarkLocationDao){
        this.bookmarkLocationDao = bookmarkLocationDao;
    }

    @Override
    public void onLocationChangedSignificantly(LatLng latLng) {
        updateMap(LOCATION_SIGNIFICANTLY_CHANGED);
    }

    @Override
    public void onLocationChangedSlightly(LatLng latLng) {

    }

    @Override
    public void onLocationChangedMedium(LatLng latLng) {

    }

    @Override
    public void updateMap(LocationChangeType locationChangeType) {
        //TODO: write inside of all methods nesli
        Timber.d("Presenter updates map and list" + locationChangeType.toString());
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns");
            return;
        }

        if (!exploreMapFragmentView.isNetworkConnectionEstablished()) {
            //TODO nesli they are closed for now, open later Timber.d("Network connection is not established");
            //TODO nesli they are closed for now, open later and add alert for network return;
            Timber.d("Network connection is not established");
            return;
        }

        LatLng lastLocation = exploreMapFragmentView.getLastLocation();
        curLatLng = lastLocation;

        if (curLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable");
            return;
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType.equals(LOCATION_SIGNIFICANTLY_CHANGED)) {
            Timber.d("LOCATION_SIGNIFICANTLY_CHANGED");
            lockUnlockNearby(true);
            //exploreMapFragmentView.setProgressBarVisibility(true);
            exploreMapFragmentView.populatePlaces(lastLocation);

        } else if (locationChangeType.equals(SEARCH_CUSTOM_AREA)) {
            Timber.d("SEARCH_CUSTOM_AREA");
            lockUnlockNearby(true);
            exploreMapFragmentView.setProgressBarVisibility(true);
            exploreMapFragmentView.populatePlaces(exploreMapFragmentView.getCameraTarget());
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly");
            if (exploreMapFragmentView.isCurrentLocationMarkerVisible()){ // Means user wants to see their live location
                exploreMapFragmentView.recenterMap(curLatLng);
            }
        }
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
            exploreMapFragmentView.disableFABRecenter();
        } else {
            exploreMapFragmentView.enableFABRecenter();
        }
    }

    @Override
    public void attachView(ExploreMapContract.View view) {
        exploreMapFragmentView = view;
        //TODO we have two attach detach method, give better namings
    }

    @Override
    public void detachView() {
        exploreMapFragmentView = DUMMY;
    }

    @Override
    public void setActionListeners(JsonKvStore applicationKvStore) {

    }

    @Override
    public void removeNearbyPreferences(JsonKvStore applicationKvStore) {

    }

    @Override
    public boolean backButtonClicked() {
        return false;
    }

    @Override
    public void onCameraMove(com.mapbox.mapboxsdk.geometry.LatLng latLng) {
        exploreMapFragmentView.setProjectorLatLngBounds();
        // If our nearby markers are calculated at least once
        if (exploreMapController.latestSearchLocation != null) {
            double distance = latLng.distanceTo
                (LocationUtils.commonsLatLngToMapBoxLatLng(exploreMapController.latestSearchLocation));
            if (exploreMapFragmentView.isNetworkConnectionEstablished()) {
                if (distance > exploreMapController.latestSearchRadius) {
                    exploreMapFragmentView.setSearchThisAreaButtonVisibility(true);
                } else {
                    exploreMapFragmentView.setSearchThisAreaButtonVisibility(false);
                }
            }
        } else {
            exploreMapFragmentView.setSearchThisAreaButtonVisibility(false);
        }
    }

    @Override
    public void updateMapMarkersToController(List<NearbyBaseMarker> nearbyBaseMarkers) {

    }

    @Override
    public void searchViewGainedFocus() {

    }

    public void onMapReady(boolean isFromSearchActivity, ExploreMapController exploreMapController, String query) {
        this.isFromSearchActivity = isFromSearchActivity;
        this.exploreMapController = exploreMapController;
        this.query = query;
        if (isFromSearchActivity && query.isEmpty()) {
            return;
        }
        if(null != exploreMapFragmentView) {
            exploreMapFragmentView.addSearchThisAreaButtonAction();
            initializeMapOperations();
        }
    }

    public void initializeMapOperations() {
        lockUnlockNearby(false);
        updateMap(LOCATION_SIGNIFICANTLY_CHANGED);
        exploreMapFragmentView.addSearchThisAreaButtonAction();
    }

    public Observable<ExplorePlacesInfo> loadAttractionsFromLocation(LatLng curLatLng, LatLng searchLatLng, boolean checkingAroundCurrent, boolean isFromSearchActivity, String query) {
        // TODO: Load attractionsta yapılanı onquery updated ile yapmanın yolunu bul
        return Observable
            .fromCallable(() -> exploreMapController
                .loadAttractionsFromLocation(curLatLng, searchLatLng,checkingAroundCurrent, isFromSearchActivity, query));
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     * @param explorePlacesInfo This variable has placeToCenter list information and distances.
     */
    public void updateMapMarkers(
        MapController.ExplorePlacesInfo explorePlacesInfo, Marker selectedMarker, boolean shouldTrackPosition) {
        Log.d("nesli2","updateMap marker with nearby place info:" + explorePlacesInfo);
        exploreMapFragmentView.setMapBoundaries(CameraUpdateFactory.newLatLngBounds(getLatLngBounds(explorePlacesInfo.boundaryCoordinates), 10));
        if(null != exploreMapFragmentView) {
            List<NearbyBaseMarker> nearbyBaseMarkers = exploreMapController
                .loadAttractionsFromLocationToBaseMarkerOptions(explorePlacesInfo.curLatLng, // Curlatlang will be used to calculate distances
                    explorePlacesInfo.explorePlaceList,
                    exploreMapFragmentView.getContext(),
                    bookmarkLocationDao.getAllBookmarksLocations());
            exploreMapFragmentView.addNearbyMarkersToMapBoxMap(nearbyBaseMarkers, selectedMarker);
            exploreMapFragmentView.addCurrentLocationMarker(explorePlacesInfo.curLatLng);
            if(shouldTrackPosition){
                exploreMapFragmentView.updateMapToTrackPosition(explorePlacesInfo.curLatLng);
            }
            lockUnlockNearby(false); // So that new location updates wont come
            exploreMapFragmentView.setProgressBarVisibility(false);
            handleCenteringTaskIfAny();
        }
    }

    private LatLngBounds getLatLngBounds(LatLng[] boundaries) {
        LatLngBounds latLngBounds = new LatLngBounds.Builder()
            .include(LocationUtils.commonsLatLngToMapBoxLatLng(boundaries[0]))
            .include(LocationUtils.commonsLatLngToMapBoxLatLng(boundaries[1]))
            .include(LocationUtils.commonsLatLngToMapBoxLatLng(boundaries[2]))
            .include(LocationUtils.commonsLatLngToMapBoxLatLng(boundaries[3]))
            .build();
        return latLngBounds;
    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before
     * map is ready. So we will remember it when the map is ready
     */
    private void handleCenteringTaskIfAny() {
        if (!placesLoadedOnce) {
            placesLoadedOnce = true;
            exploreMapFragmentView.centerMapToPlace(null);
        }
    }
}
