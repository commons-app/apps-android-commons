package fr.free.nrw.commons.explore.map

import android.content.Context
import android.view.View
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType

class ExploreMapContract {
    /** Actions related to the bottom details sheet. */
    interface BottomSheet {
        fun hideBottomDetailsSheet()

        fun isDetailsBottomSheetVisible(): Boolean

        fun backButtonClicked(): Boolean
    }

    /** Actions related to map markers and overlays. */
    interface MarkerOverlay {
        fun addMarkersToMap(nearbyBaseMarkers: List<BaseMarker?>?)

        fun clearAllMarkers()
    }

    /** Actions related to camera viewport, focus, and recenter button. */
    interface Viewport {
        fun getMapCenter(): LatLng?

        fun getMapFocus(): LatLng?

        fun getLastMapFocus(): LatLng?

        fun recenterMap(curLatLng: LatLng?)

        fun disableFABRecenter()

        fun enableFABRecenter()

        fun setFABRecenterAction(onClickListener: android.view.View.OnClickListener?)

        fun addSearchThisAreaButtonAction()

        fun setSearchThisAreaButtonVisibility(isVisible: Boolean)

        fun isSearchThisAreaButtonVisible(): Boolean
    }

    /** Actions related to device location and network status. */
    interface Location {
        fun askForLocationPermission()

        fun getLastLocation(): LatLng?

        fun isNetworkConnectionEstablished(): Boolean
    }

    interface View :
        BottomSheet,
        MarkerOverlay,
        Viewport,
        Location {
        fun populatePlaces(curlatLng: LatLng?)

        fun setProgressBarVisibility(isVisible: Boolean)

        fun getContext(): Context?
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
