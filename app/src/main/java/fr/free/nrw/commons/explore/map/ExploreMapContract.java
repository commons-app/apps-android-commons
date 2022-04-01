package fr.free.nrw.commons.explore.map;

import android.content.Context;
import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.camera.CameraUpdate;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import java.util.List;

public class ExploreMapContract {

    interface View {
        boolean isNetworkConnectionEstablished();
        void populatePlaces(LatLng curlatLng,LatLng searchLatLng);
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
        void disableFABRecenter();
        void enableFABRecenter();
        void setCustomQuery(String customQuery);
        void addNearbyMarkersToMapBoxMap(final List<NearbyBaseMarker> nearbyBaseMarkers, final Marker selectedMarker);
        void setMapBoundaries(CameraUpdate cameaUpdate);
        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);
        boolean backButtonClicked();
    }

    interface UserActions {
        void updateMap(LocationServiceManager.LocationChangeType locationChangeType);
        void lockUnlockNearby(boolean isNearbyLocked);
        void attachView(View view);
        void detachView();
        void setActionListeners(JsonKvStore applicationKvStore);
        void removeNearbyPreferences(JsonKvStore applicationKvStore);
        boolean backButtonClicked();
        void onCameraMove(com.mapbox.mapboxsdk.geometry.LatLng latLng);
        void markerUnselected();
        void markerSelected(Marker marker);
    }

}
