package fr.free.nrw.commons.explore.map

import android.content.Context
import android.view.View
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType

class ExploreMapContract {
    interface View {
        fun isNetworkConnectionEstablished(): Boolean
        fun populatePlaces(curlatLng: LatLng?)
        fun askForLocationPermission()
        fun recenterMap(curLatLng: LatLng?)
        fun hideBottomDetailsSheet()
        fun getMapCenter(): LatLng?
        fun getMapFocus(): LatLng?
        fun getLastMapFocus(): LatLng?
        fun addMarkersToMap(nearbyBaseMarkers: List<BaseMarker?>?)
        fun clearAllMarkers()
        fun addSearchThisAreaButtonAction()
        fun setSearchThisAreaButtonVisibility(isVisible: Boolean)
        fun setProgressBarVisibility(isVisible: Boolean)
        fun isDetailsBottomSheetVisible(): Boolean
        fun isSearchThisAreaButtonVisible(): Boolean
        fun getContext(): Context?
        fun getLastLocation(): LatLng?
        fun disableFABRecenter()
        fun enableFABRecenter()
        fun setFABRecenterAction(onClickListener: android.view.View.OnClickListener?)
        fun backButtonClicked(): Boolean
    }

    interface UserActions {
        fun updateMap(locationChangeType: LocationChangeType)
        fun lockUnlockNearby(isNearbyLocked: Boolean)
        fun attachView(view: View?)
        fun detachView()
        fun setActionListeners(applicationKvStore: JsonKvStore?)
        fun backButtonClicked(): Boolean
    }
}
