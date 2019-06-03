package fr.free.nrw.commons.nearby.mvp.contract;

import android.net.Uri;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.mapbox.mapboxsdk.annotations.BaseMarkerOptions;
import com.mapbox.mapboxsdk.maps.MapView;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;

/**
 * This interface defines specific View and UserActions for map
 * part of the nearby. On the other hand both extends methods
 * from parent View and UserActions where general methods are
 * defined (in Nearby Contract)
 */
public interface NearbyMapContract {

    interface View extends NearbyElementContract.View{
        void showSearchThisAreaButton();
        void showInformationBottomSheet();
        void initViews();
        void updateMapMarkers(LatLng latLng, List<Place> placeList);
        void updateMapToTrackPosition(LatLng curLatLng);
        void setListeners();
        MapView setupMapView(Bundle savedInstanceState);
        void addCurrentLocationMarker(LatLng curLatLng);
        void setSearchThisAreaButtonVisibility(boolean visible);
        boolean isCurrentLocationMarkerVisible();
        void addNearbyMarkersToMapBoxMap(List<NearbyBaseMarker> baseMarkerOptions);
        void prepareViewsForSheetPosition();
        void hideFABs();
        void showFABs();
        void addAnchorToBigFABs(FloatingActionButton floatingActionButton, int anchorID);
        void removeAnchorFromFABs(FloatingActionButton fab);
        void  addAnchorToSmallFABs(FloatingActionButton floatingActionButton, int anchorID);
        void passInfoToSheet(Place place);
        void updateBookmarkButtonImage(Place place);
        void openWebView(Uri link);
        void animateFABs(boolean isFabOpen);
        void closeFabs ( boolean isFabOpen);
        void updateMarker(boolean isBookmarked, Place place);
        LatLng getCameraTarget();
        MapboxMap getMapboxMap();
        void viewsAreAssignedToPresenter(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback);
        void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener);
    }

    interface UserActions extends NearbyElementContract.UserActions {
        void searchThisArea();
        void storeSharedPrefs();
        void recenterMap();
        void updateMapMarkers(LatLng latLng);
        void updateMapToTrackPosition();
        void getBundleContent();
    }
}
