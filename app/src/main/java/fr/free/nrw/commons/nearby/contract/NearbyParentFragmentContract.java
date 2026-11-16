package fr.free.nrw.commons.nearby.contract;

import android.content.Context;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleCoroutineScope;
import fr.free.nrw.commons.BaseMarker;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType;
import fr.free.nrw.commons.nearby.Label;
import fr.free.nrw.commons.nearby.MarkerPlaceGroup;
import fr.free.nrw.commons.nearby.Place;
import java.util.List;

public interface NearbyParentFragmentContract {

    /** Actions related to exporting places to KML and GPX files. */
    interface FileExport {

        void savePlacesAsKML();

        void savePlacesAsGPX();
    }

    /** Actions related to map markers and overlays. */
    interface MarkerOverlay {

        void addCurrentLocationMarker(LatLng currentLatLng);

        void clearAllMarkers();

        void replaceMarkerOverlays(List<MarkerPlaceGroup> markerPlaceGroups);

        void filterOutAllMarkers();

        void filterMarkersByLabels(List<Label> selectedLabels, boolean filterForPlaceState,
            boolean filterForAllNoneType);
    }

    /** Actions related to list and details bottom sheets. */
    interface BottomSheet {

        boolean isListBottomSheetExpanded();

        boolean isDetailsBottomSheetVisible();

        void hideBottomSheet();

        void hideBottomDetailsSheet();

        void setBottomSheetDetailsSmaller();

        void updateListFragment(List<Place> placeList);
    }

    /** Actions related to device location services, permissions, and location center. */
    interface Location {

        void askForLocationPermission();

        void recenterMap(LatLng currentLatLng);

        void openLocationSettings();

        LatLng getLastLocation();

        LatLng getMapCenter();
    }

    /** Actions related to map camera viewport, focus, and screen coordinates. */
    interface Viewport {

        LatLng getCameraTarget();

        void centerMapToPlace(@Nullable Place placeToCenter);

        LatLng getLastMapFocus();

        LatLng getMapFocus();

        LatLng getScreenTopRight();

        LatLng getScreenBottomLeft();
    }

    interface View extends FileExport, MarkerOverlay, BottomSheet, Location, Viewport {

        boolean isNetworkConnectionEstablished();

        void updateSnackbar(boolean offlinePinsShown);

        void listOptionMenuItemClicked();

        void populatePlaces(LatLng currentLatLng);

        void populatePlaces(LatLng currentLatLng, String customQuery);

        void displayLoginSkippedWarning();

        void setFABPlusAction(android.view.View.OnClickListener onClickListener);

        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);

        void animateFABs();

        void setProgressBarVisibility(boolean isVisible);

        boolean isNearbyFilterListVisible();

        void hideNearbyFilterList();

        void setRecyclerViewAdapterAllSelected();

        void setRecyclerViewAdapterItemsGreyedOut();

        void setCheckBoxAction();

        void setCheckBoxState(int state);

        void setFilterState();

        void disableFABRecenter();

        void enableFABRecenter();

        Context getContext();

        boolean isAdvancedQueryFragmentVisible();

        void showHideAdvancedQueryFragment(boolean shouldShow);

        void stopQuery();
    }

    interface NearbyListView {

        void updateListFragment(List<Place> placeList);
    }

    interface UserActions {

        void updateMapAndList(LocationChangeType locationChangeType);

        void lockUnlockNearby(boolean isNearbyLocked);

        void attachView(View view);

        void detachView();

        void setActionListeners(JsonKvStore applicationKvStore);

        void removeNearbyPreferences(JsonKvStore applicationKvStore);

        boolean backButtonClicked();

        void filterByMarkerType(List<Label> selectedLabels, int state, boolean filterForPlaceState,
            boolean filterForAllNoneType);

        void searchViewGainedFocus();

        void setCheckboxUnknown();

        void setAdvancedQuery(String query);

        void toggleBookmarkedStatus(Place place, LifecycleCoroutineScope scope);

        void handleMapScrolled(LifecycleCoroutineScope scope, boolean isNetworkAvailable);
    }
}
