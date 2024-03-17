package fr.free.nrw.commons.explore.map;

import android.content.Context;
import com.mapbox.mapboxsdk.annotations.Marker;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import java.util.List;

public class ExploreMapContract {

    interface View {
        boolean isNetworkConnectionEstablished();
        void populatePlaces(LatLng curlatLng);
        void checkPermissionsAndPerformAction();
        void recenterMap(LatLng curLatLng);
        void showLocationOffDialog();
        void openLocationSettings();
        void hideBottomDetailsSheet();
        void displayBottomSheetWithInfo(Marker marker);
        LatLng getMapCenter();
        LatLng getMapFocus();
        LatLng getLastMapFocus();
        void addMarkersToMap(final List<NearbyBaseMarker> nearbyBaseMarkers);
        void clearAllMarkers();
        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setProgressBarVisibility(boolean isVisible);
        boolean isDetailsBottomSheetVisible();
        boolean isSearchThisAreaButtonVisible();
        Context getContext();
        LatLng getLastLocation();
        void disableFABRecenter();
        void enableFABRecenter();
        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);
        boolean backButtonClicked();
    }

    interface UserActions {
        void updateMap(LocationServiceManager.LocationChangeType locationChangeType);
        void lockUnlockNearby(boolean isNearbyLocked);
        void attachView(View view);
        void detachView();
        void setActionListeners(JsonKvStore applicationKvStore);
        boolean backButtonClicked();
    }

}
