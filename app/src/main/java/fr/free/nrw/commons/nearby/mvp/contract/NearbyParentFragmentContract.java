package fr.free.nrw.commons.nearby.mvp.contract;

import android.view.View;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;

public interface NearbyParentFragmentContract {

    interface View {
        void setListFragmentExpanded();
        void refreshView();
        void registerLocationUpdates(LocationServiceManager locationServiceManager);
        boolean isNetworkConnectionEstablished();
        void addNetworkBroadcastReceiver();
        void listOptionMenuItemClicked();
        void  populatePlaces(LatLng curlatLng, LatLng searchLatLng);
        boolean isBottomSheetExpanded();
        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setSearchThisAreaProgressVisibility(boolean isVisible);
        void checkPermissionsAndPerformAction(Runnable runnable);
        void resumeFragment();
        void displayLoginSkippedWarning();
        void setFABPlusAction(android.view.View.OnClickListener onClickListener);
        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);
        void animateFABs();
        void recenterMap(LatLng curLatLng);
        void initViewPositions();
        void hideBottomSheet();
        void displayBottomSheetWithInfo(Marker marker);
    }

    interface UserActions {
        void displayListFragmentExpanded();
        void onTabSelected();
        void initializeNearbyOperations();
        void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType, LatLng cameraTarget);
        void lockNearby(boolean isNearbyLocked);
        MapboxMap.OnCameraMoveListener onCameraMove(MapboxMap mapboxMap);
        void setActionListeners(JsonKvStore applicationKvStore);
    }
    
    interface ViewsAreReadyCallback {
        void nearbyFragmentsAreReady();
        void nearbyMapViewReady();
    }
}
