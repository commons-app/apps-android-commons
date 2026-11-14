package fr.free.nrw.commons.explore.map

import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.explore.map.ExploreMapController.Companion.loadAttractionsFromLocationToBaseMarkerOptions

/**
 * Owns thumbnail preparation and completion while resolving the presenter's current view lazily.
 * Late callbacks therefore follow the presenter's attachment state instead of a captured view.
 */
internal class ExploreMapMarkerHandler : ExploreMapMarkerActions {
    private lateinit var view: () -> ExploreMapContract.View?
    private lateinit var lockUnlockNearby: (Boolean) -> Unit

    /** Connects marker completion to the same view and locking state used by map searches. */
    fun bind(
        view: () -> ExploreMapContract.View?,
        lockUnlockNearby: (Boolean) -> Unit,
    ) {
        this.view = view
        this.lockUnlockNearby = lockUnlockNearby
    }

    /**
     * Populates places for custom location, should be used for finding nearby places around a
     * location where you are not at.
     *
     * @param explorePlacesInfo This variable has placeToCenter list information and distances.
     */
    override fun updateMapMarkers(explorePlacesInfo: ExplorePlacesInfo) {
        if (explorePlacesInfo.mediaList != null) {
            prepareNearbyBaseMarkers(explorePlacesInfo)
        } else {
            lockUnlockNearby(false) // So that new location updates wont come
            view()!!.setProgressBarVisibility(false)
        }
    }

    /** Supplies this handler as the callback for the controller's asynchronous thumbnail loading. */
    private fun prepareNearbyBaseMarkers(explorePlacesInfo: ExplorePlacesInfo) {
        loadAttractionsFromLocationToBaseMarkerOptions(
            explorePlacesInfo.currentLatLng, // Curlatlang will be used to calculate distances
            explorePlacesInfo.explorePlaceList,
            view()!!.getContext()!!,
            this,
            explorePlacesInfo,
        )
    }

    /** Completes loading against the current view rather than the view that started the request. */
    override fun onNearbyBaseMarkerThumbsReady(
        baseMarkers: List<BaseMarker>?,
        explorePlacesInfo: ExplorePlacesInfo?,
    ) {
        if (null != view()) {
            view()!!.addMarkersToMap(baseMarkers)
            lockUnlockNearby(false) // So that new location updates wont come
            view()!!.setProgressBarVisibility(false)
        }
    }
}
