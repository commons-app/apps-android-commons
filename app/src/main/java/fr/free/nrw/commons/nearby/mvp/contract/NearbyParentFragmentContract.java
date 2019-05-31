package fr.free.nrw.commons.nearby.mvp.contract;


import com.mapbox.mapboxsdk.maps.MapboxMap;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;

public interface NearbyParentFragmentContract {

    interface View {
        void setListFragmentExpanded();
        void refreshView();
        void registerLocationUpdates(LocationServiceManager locationServiceManager);
        void requestLocationPermissions(LocationServiceManager locationServiceManager);
        void showLocationPermissionDeniedErrorDialog(LocationServiceManager locationServiceManager);
        void checkGps(LocationServiceManager locationServiceManager);
        void checkLocationPermission(LocationServiceManager locationServiceManager);
        boolean isNetworkConnectionEstablished();
        void addNetworkBroadcastReceiver();
        void listOptionMenuItemClicked();
        void  populatePlaces(LatLng curlatLng, LatLng searchLatLng);
        boolean isBottomSheetExpanded();

        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setSearchThisAreaProgressVisibility(boolean isVisible);
    }

    interface UserActions {
        void displayListFragmentExpanded();

        void onTabSelected();

        void initializeNearbyOperations();

        void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType, LatLng cameraTarget);

        void lockNearby(boolean isNearbyLocked);

        void addMapMovementListeners();

        MapboxMap.OnCameraMoveListener onCameraMove(LatLng cameraTarget);
    }
    
    interface ViewsAreReadyCallback {
        void nearbyFragmentsAreReady();
        void nearbyMapViewReady();
    }
}
