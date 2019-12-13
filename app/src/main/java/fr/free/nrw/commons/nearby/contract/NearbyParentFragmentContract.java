package fr.free.nrw.commons.nearby.contract;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.Place;

public interface NearbyParentFragmentContract {

    interface View {
        void registerLocationUpdates(LocationServiceManager locationServiceManager);
        boolean isNetworkConnectionEstablished();
        void addNetworkBroadcastReceiver();
        void listOptionMenuItemClicked();
        void  populatePlaces(LatLng curlatLng, LatLng searchLatLng);
        boolean isListBottomSheetExpanded();
        void checkPermissionsAndPerformAction(Runnable runnable);
        void displayLoginSkippedWarning();
        void setFABPlusAction(android.view.View.OnClickListener onClickListener);
        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);
        void animateFABs();
        void recenterMap(LatLng curLatLng);
        void hideBottomSheet();
        void hideBottomDetailsSheet();
        void displayBottomSheetWithInfo(Marker marker);
        void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener);
        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setProgressBarVisibility(boolean isVisible);
        void setTabItemContributions();
        boolean isDetailsBottomSheetVisible();
        void setBottomSheetDetailsSmaller();
        boolean isSearchThisAreaButtonVisible();
        void setRecyclerViewAdapterAllSelected();
        void setRecyclerViewAdapterItemsGreyedOut();
        void setCheckBoxAction();
        void setCheckBoxState(int state);
        void setFilterState();
        void disableFABRecenter();
        void enableFABRecenter();
    }

    interface NearbyListView {
        void updateListFragment(List<Place> placeList);
    }

    interface UserActions {
        void onTabSelected();
        void checkForPermission();
        void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType, LatLng cameraTarget);
        void lockUnlockNearby(boolean isNearbyLocked);
        void setActionListeners(JsonKvStore applicationKvStore);
        void backButtonClicked();
        MapboxMap.OnCameraMoveListener onCameraMove(MapboxMap mapboxMap);
        void filterByMarkerType(List<Label> selectedLabels, int state, boolean filterForPlaceState, boolean filterForAllNoneType);
        void searchViewGainedFocus();
        void setCheckboxUnknown();
    }
    
    interface ViewsAreReadyCallback {
        void nearbyFragmentsAreReady();
    }
}
