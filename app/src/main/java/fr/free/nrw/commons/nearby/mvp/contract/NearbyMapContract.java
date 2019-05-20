package fr.free.nrw.commons.nearby.mvp.contract;

import android.net.Uri;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

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
        void updateMapMarkers();
        void updateMapToTrackPosition();
        void setListeners();
        void setupMapView();
        void addCurrentLocationMarker();
        void setSearchThisAreaButtonVisibility(boolean visible);
        boolean isCurrentLocationMarkerVisible();
        void addNearbyMarkersToMapBoxMap();
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
    }

    interface UserActions extends NearbyElementContract.UserActions {
        void searchThisArea();
        void storeSharedPrefs();
        void recenterMap();
        void updateMapMarkers();
        void updateMapToTrackPosition();
        void getBundleContent();
        boolean addMapMovementListener();
    }
}
