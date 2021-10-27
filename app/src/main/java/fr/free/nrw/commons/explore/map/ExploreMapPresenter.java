package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import fr.free.nrw.commons.location.LocationUpdateListener;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract.View;
import java.util.List;

public class ExploreMapPresenter
    implements ExploreMapContract.UserActions,
    LocationUpdateListener {
    BookmarkLocationsDao bookmarkLocationDao;

    public ExploreMapPresenter(BookmarkLocationsDao bookmarkLocationDao){
        this.bookmarkLocationDao=bookmarkLocationDao;
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

    @Override
    public void updateMapAndList(LocationChangeType locationChangeType) {

    }

    @Override
    public void lockUnlockNearby(boolean isNearbyLocked) {

    }

    @Override
    public void attachView(View view) {

    }

    @Override
    public void detachView() {

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

    }

    @Override
    public void updateMapMarkersToController(List<NearbyBaseMarker> nearbyBaseMarkers) {

    }

    @Override
    public void searchViewGainedFocus() {

    }

}
