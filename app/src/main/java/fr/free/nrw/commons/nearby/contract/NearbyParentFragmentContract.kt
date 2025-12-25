package fr.free.nrw.commons.nearby.contract

import android.content.Context
import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place

interface NearbyParentFragmentContract {
    interface View {
        val isNetworkConnectionEstablished: Boolean

        fun updateSnackbar(offlinePinsShown: Boolean)

        fun listOptionMenuItemClicked()

        fun populatePlaces(currentLatLng: LatLng)

        fun populatePlaces(currentLatLng: LatLng, customQuery: String?)

        val isListBottomSheetExpanded: Boolean

        fun askForLocationPermission()

        fun displayLoginSkippedWarning()

        fun setFABPlusAction(onClickListener: android.view.View.OnClickListener?)

        fun setFABRecenterAction(onClickListener: android.view.View.OnClickListener?)

        fun animateFABs()

        fun recenterMap(currentLatLng: LatLng?)

        fun openLocationSettings()

        fun hideBottomSheet()

        fun hideBottomDetailsSheet()

        fun setProgressBarVisibility(isVisible: Boolean)

        val isDetailsBottomSheetVisible: Boolean

        fun setBottomSheetDetailsSmaller()

        fun setRecyclerViewAdapterAllSelected()

        fun setRecyclerViewAdapterItemsGreyedOut()

        fun setCheckBoxAction()

        fun setCheckBoxState(state: Int)

        fun setFilterState()

        fun disableFABRecenter()

        fun enableFABRecenter()

        fun addCurrentLocationMarker(currentLatLng: LatLng?)

        fun clearAllMarkers()

        fun getContext(): Context?

        fun replaceMarkerOverlays(markerPlaceGroups: List<MarkerPlaceGroup>)

        fun filterOutAllMarkers()

        fun filterMarkersByLabels(
            selectedLabels: List<Label>?, filterForPlaceState: Boolean, filterForAllNoneType: Boolean
        )

        val cameraTarget: LatLng?

        fun centerMapToPlace(placeToCenter: Place?)

        fun updateListFragment(placeList: List<Place>?)

        fun getLastLocation(): LatLng

        fun getLastMapFocus(): LatLng

        fun getMapCenter(): LatLng

        fun getMapFocus(): LatLng

        fun getScreenTopRight(): LatLng

        fun getScreenBottomLeft(): LatLng

        fun isAdvancedQueryFragmentVisible(): Boolean

        fun showHideAdvancedQueryFragment(shouldShow: Boolean)

        fun stopQuery()
    }

    interface NearbyListView {
        fun updateListFragment(placeList: List<Place>?)
    }

    interface UserActions : CheckBoxTriStates.Callback {
        fun updateMapAndList(locationChangeType: LocationChangeType?)

        fun lockUnlockNearby(isNearbyLocked: Boolean)

        fun attachView(view: View)

        fun detachView()

        fun setActionListeners(applicationKvStore: JsonKvStore?)

        fun removeNearbyPreferences(applicationKvStore: JsonKvStore?)

        fun backButtonClicked(): Boolean

        override fun filterByMarkerType(
            selectedLabels: List<Label>?, state: Int, filterForPlaceState: Boolean,
            filterForAllNoneType: Boolean
        )

        fun searchViewGainedFocus()

        fun setCheckboxUnknown()

        fun setAdvancedQuery(query: String?)

        fun toggleBookmarkedStatus(place: Place?, scope: LifecycleCoroutineScope?)

        fun handleMapScrolled(scope: LifecycleCoroutineScope?, isNetworkAvailable: Boolean)
    }
}
