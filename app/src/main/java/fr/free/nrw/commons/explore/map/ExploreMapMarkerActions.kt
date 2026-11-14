package fr.free.nrw.commons.explore.map

import fr.free.nrw.commons.MapController.ExplorePlacesInfo
import fr.free.nrw.commons.explore.map.ExploreMapController.NearbyBaseMarkerThumbCallback

/** Keeps marker loading callable through the presenter while its implementation has a separate owner. */
interface ExploreMapMarkerActions : NearbyBaseMarkerThumbCallback {
    /** Starts preparing thumbnails for the places returned by the current map search. */
    fun updateMapMarkers(explorePlacesInfo: ExplorePlacesInfo)
}
