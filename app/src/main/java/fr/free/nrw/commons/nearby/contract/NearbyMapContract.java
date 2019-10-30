package fr.free.nrw.commons.nearby.contract;

import com.mapbox.mapboxsdk.annotations.Marker;
import com.mapbox.mapboxsdk.maps.MapboxMap;

import java.util.List;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.NearbyBaseMarker;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.nearby.presenter.NearbyParentFragmentPresenter;

/**
 * This interface defines specific View and UserActions for map
 * part of the nearby.
 */
public interface NearbyMapContract {

    interface View{
        void updateMapMarkers(LatLng latLng, List<Place> placeList, Marker selectedMarker, NearbyParentFragmentPresenter nearbyParentFragmentPresenter);
        void updateMapToTrackPosition(LatLng curLatLng);
        void addCurrentLocationMarker(LatLng curLatLng);
        void addNearbyMarkersToMapBoxMap(List<NearbyBaseMarker> baseMarkerOptions, Marker marker, NearbyParentFragmentPresenter nearbyParentFragmentPresenter);
        LatLng getCameraTarget();
        MapboxMap getMapboxMap();
        void viewsAreAssignedToPresenter(NearbyParentFragmentContract.ViewsAreReadyCallback viewsAreReadyCallback);
        void addOnCameraMoveListener(MapboxMap.OnCameraMoveListener onCameraMoveListener);
        void centerMapToPlace(Place place, boolean isPortraitMode);
        void removeCurrentLocationMarker();
        List<NearbyBaseMarker> getBaseMarkerOptions();
        void filterMarkersByLabels(List<Label> labelList, boolean displayExists, boolean displayNeeds, boolean filterForPlaceState, boolean filterForAllNoneType);
        void filterOutAllMarkers();
        void displayAllMarkers();
    }
}
