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

    interface View {

        boolean isNetworkConnectionEstablished();

        void updateSnackbar(boolean offlinePinsShown);

        void listOptionMenuItemClicked();

        void populatePlaces(LatLng currentLatLng);

        void populatePlaces(LatLng currentLatLng, String customQuery);

        boolean isListBottomSheetExpanded();

        void askForLocationPermission();

        void displayLoginSkippedWarning();

        void setFABPlusAction(android.view.View.OnClickListener onClickListener);

        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);

        void animateFABs();

        void recenterMap(LatLng currentLatLng);

        void openLocationSettings();

        void hideBottomSheet();

        void hideBottomDetailsSheet();

        void setProgressBarVisibility(boolean isVisible);

        boolean isDetailsBottomSheetVisible();

        void setBottomSheetDetailsSmaller();

        void setRecyclerViewAdapterAllSelected();

        void setRecyclerViewAdapterItemsGreyedOut();

        void setCheckBoxAction();

        void setCheckBoxState(int state);

        void setFilterState();

        void disableFABRecenter();

        void enableFABRecenter();

        void addCurrentLocationMarker(LatLng currentLatLng);

        void clearAllMarkers();

        Context getContext();

        void replaceMarkerOverlays(List<MarkerPlaceGroup> markerPlaceGroups);

        void filterOutAllMarkers();

        void filterMarkersByLabels(List<Label> selectedLabels, boolean filterForPlaceState,
            boolean filterForAllNoneType);

        LatLng getCameraTarget();

        void centerMapToPlace(@Nullable Place placeToCenter);

        void updateListFragment(List<Place> placeList);

        LatLng getLastLocation();

        LatLng getLastMapFocus();

        LatLng getMapCenter();

        LatLng getMapFocus();

        LatLng getScreenTopRight();

        LatLng getScreenBottomLeft();

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
