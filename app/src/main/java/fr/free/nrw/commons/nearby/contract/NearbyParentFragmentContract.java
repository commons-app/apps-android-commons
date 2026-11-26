package fr.free.nrw.commons.nearby.contract;

import android.content.Context;
import android.os.Bundle;
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

        /**
         * Whether only places that need a photo should be searched for and shown. Always false
         * when picking a place for an upload.
         */
        boolean isOnlyNeedsPhoto();
    }

    /** Actions related to list and details bottom sheets. */
    interface BottomSheet {

        boolean isListBottomSheetExpanded();

        boolean isDetailsBottomSheetVisible();

        void hideBottomSheet();

        void hideBottomDetailsSheet();

        void setBottomSheetDetailsSmaller();

        void updateListFragment(List<Place> placeList);

        /**
         * Hide or expand bottom sheet according to states of all sheets.
         */
        void listOptionMenuItemClicked();
    }

    /** Actions related to device location services, permissions, and location center. */
    interface Location {

        void askForLocationPermission();

        void recenterMap(LatLng currentLatLng);

        void openLocationSettings();

        LatLng getLastLocation();

        LatLng getMapCenter();

        /**
         * Registers or unregisters device location updates.
         *
         * @param removeLocationListener True to unregister and remove listener, false to register
         */
        void registerUnregisterLocationListener(boolean removeLocationListener);
    }

    /** Actions related to map camera viewport, focus, and screen coordinates. */
    interface Viewport {

        LatLng getCameraTarget();

        void centerMapToPlace(@Nullable Place placeToCenter);

        LatLng getLastMapFocus();

        LatLng getMapFocus();

        LatLng getScreenTopRight();

        LatLng getScreenBottomLeft();

        /**
         * Updates camera position and refreshes places when new Explore map coordinates arrive
         * without recreating the fragment.
         */
        void onExploreMapDataReceived(Bundle bundle);
    }

    /** Actions related to place type filtering and advanced query search. */
    interface Filter {

        boolean isNearbyFilterListVisible();

        void hideNearbyFilterList();

        void setRecyclerViewAdapterAllSelected();

        void setRecyclerViewAdapterItemsGreyedOut();

        void setCheckBoxAction();

        void setCheckBoxState(int state);

        void setFilterState();

        boolean isAdvancedQueryFragmentVisible();

        void showHideAdvancedQueryFragment(boolean shouldShow);

        /**
         * Drops a custom Nearby query and loads places for the current map center again.
         */
        void resetAdvancedQuery();
    }

    /** Actions related to floating action buttons (FABs) and animations. */
    interface FabControl {

        void displayLoginSkippedWarning();

        void setFABPlusAction(android.view.View.OnClickListener onClickListener);

        void setFABRecenterAction(android.view.View.OnClickListener onClickListener);

        void animateFABs();

        void disableFABRecenter();

        void enableFABRecenter();

        /**
         * Adjusts FAB positioning and margins based on bottom sheet state.
         *
         * @param bottomSheetState see bottom sheet states
         */
        void prepareViewsForSheetPosition(int bottomSheetState);
    }

    /** Actions related to querying, loading, and stopping place searches. */
    interface PlacesQuery {

        /**
         * Populates nearby places around the specified location.
         *
         * @param currentLatLng Current location coordinates
         */
        void populatePlaces(LatLng currentLatLng);

        /**
         * Populates nearby places using a custom search query around the specified location.
         *
         * @param currentLatLng Current location coordinates
         * @param customQuery Custom SPARQL or search query string
         */
        void populatePlaces(LatLng currentLatLng, String customQuery);

        /**
         * Clears the Nearby local cache and requests places afresh.
         */
        void emptyCache();

        /**
         * Stops any ongoing queries and clears all disposables.
         */
        void stopQuery();

        /**
         * Shows that a search found no places, so no places from an earlier search stay visible.
         */
        void showNoPlaces();
    }

    /** Actions related to network status, progress display, and snackbar notifications. */
    interface StatusControl {

        /**
         * Checks whether an active internet connection is currently established.
         *
         * @return true if internet connection is established, false otherwise
         */
        boolean isNetworkConnectionEstablished();

        /**
         * Updates the snackbar message depending on whether offline cached pins are displayed.
         *
         * @param offlinePinsShown Whether offline pins are currently visible
         */
        void updateSnackbar(boolean offlinePinsShown);

        /**
         * Toggles the visibility of the map loading progress bar.
         *
         * @param isVisible true to display progress bar, false to hide
         */
        void setProgressBarVisibility(boolean isVisible);

        /**
         * Handles back button press delegation for the fragment and its children.
         *
         * @return true if event was consumed, false otherwise
         */
        boolean backButtonClicked();

        /**
         * Sets callback notified when the fragment instance views and overlays are ready.
         *
         * @param callback Ready notification callback
         */
        void setNearbyParentFragmentInstanceReadyCallback(
            fr.free.nrw.commons.nearby.fragments.NearbyParentFragment.NearbyParentFragmentInstanceReadyCallback callback);

        /**
         * Handles click on Learn More URL to open Wiki Loves Monuments guide in a browser.
         */
        void onLearnMoreClicked();
    }

    interface View extends FileExport, MarkerOverlay, BottomSheet, Location, Viewport, Filter, FabControl, PlacesQuery, StatusControl {

        Context getContext();
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

        /**
         * Searches the visible area again after "only places that need a photo" was switched,
         * since the search itself depends on it.
         */
        void onOnlyNeedsPhotoChanged();

        void toggleBookmarkedStatus(Place place, LifecycleCoroutineScope scope);

        void handleMapScrolled(LifecycleCoroutineScope scope, boolean isNetworkAvailable);
    }
}
