package fr.free.nrw.commons.explore.map;

import android.content.Context;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import java.util.List;

public class ExploreMapContract {

    interface View {
        boolean isNetworkConnectionEstablished();
        void populatePlaces(LatLng currentLatLng);
        void checkPermissionsAndPerformAction();
        void recenterMap(LatLng currentLatLng);
        void showLocationOffDialog();
        void openLocationSettings();
        void hideBottomDetailsSheet();
        LatLng getMapCenter();
        LatLng getMapFocus();
        LatLng getLastMapFocus();
        void addMarkersToMap(final List<BaseMarker> nearbyBaseMarkers);
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
