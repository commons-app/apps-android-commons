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

        void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener);
        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setProgressBarVisibility(boolean isVisible);
    }

    interface UserActions {
        void displayListFragmentExpanded();
        void onTabSelected();
        void initializeNearbyOperations();
        void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType, LatLng cameraTarget);
        void lockUnlockNearby(boolean isNearbyLocked);
        void setActionListeners(JsonKvStore applicationKvStore);

        MapboxMap.OnCameraMoveListener onCameraMove(MapboxMap mapboxMap);
    }
    
    interface ViewsAreReadyCallback {
        void nearbyFragmentsAreReady();
        void nearbyMapViewReady();
    }
}
