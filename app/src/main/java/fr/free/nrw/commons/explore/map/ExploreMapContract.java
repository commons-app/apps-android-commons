package fr.free.nrw.commons.explore.map;

import android.content.Context;
import com.mapbox.mapboxsdk.annotations.Marker;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
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
        void addOnCameraMoveListener();
        void addSearchThisAreaButtonAction();
        void setSearchThisAreaButtonVisibility(boolean isVisible);
        void setProgressBarVisibility(boolean isVisible);
        boolean isDetailsBottomSheetVisible();
        void setBottomSheetDetailsSmaller();
        boolean isSearchThisAreaButtonVisible();
        void addCurrentLocationMarker(LatLng curLatLng);
        void updateMapToTrackPosition(LatLng curLatLng);
        Context getContext();
        void updateMapMarkers(List<NearbyBaseMarker> nearbyBaseMarkers, Marker selectedMarker);
        LatLng getCameraTarget();
        void centerMapToPlace(Place placeToCenter);
        LatLng getLastLocation();
        com.mapbox.mapboxsdk.geometry.LatLng getLastFocusLocation();
        boolean isCurrentLocationMarkerVisible();
        void setProjectorLatLngBounds();
    }

    interface UserActions {
        void updateMapAndList(LocationServiceManager.LocationChangeType locationChangeType);
        void lockUnlockNearby(boolean isNearbyLocked);
        void attachView(View view);
        void detachView();
        void setActionListeners(JsonKvStore applicationKvStore);
        void removeNearbyPreferences(JsonKvStore applicationKvStore);
        boolean backButtonClicked();
        void onCameraMove(com.mapbox.mapboxsdk.geometry.LatLng latLng);
        void updateMapMarkersToController(List<NearbyBaseMarker> nearbyBaseMarkers);
        void searchViewGainedFocus();
    }

}
