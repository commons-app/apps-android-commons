package fr.free.nrw.commons.nearby.mvp.contract;


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
    }

    interface UserActions {
        void displayListFragmentExpanded();
        void onTabSelected();
        void initializeNearbyOperations();
    }
}
