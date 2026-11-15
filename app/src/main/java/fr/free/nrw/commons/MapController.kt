package fr.free.nrw.commons

import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place

abstract class MapController {
    /**
     * We pass this variable as a group of placeList and boundaryCoordinates
     */
    inner class NearbyPlacesInfo {
        @JvmField
        var placeList: List<Place> = emptyList() // List of nearby places

        @JvmField
        var boundaryCoordinates: Array<LatLng> = emptyArray() // Corners of nearby area

        @JvmField
        var currentLatLng: LatLng? = null // Current location when this places are populated

        @JvmField
        var searchLatLng: LatLng? = null // Search location for finding this places

        @JvmField
        var mediaList: List<Media>? = null // Search location for finding this places
    }

    /**
     * We pass this variable as a group of placeList and boundaryCoordinates
     */
    inner class ExplorePlacesInfo {
        @JvmField
        var explorePlaceList: List<Place> = emptyList() // List of nearby places

        @JvmField
        var boundaryCoordinates: Array<LatLng> = emptyArray() // Corners of nearby area

        @JvmField
        var currentLatLng: LatLng? = null // Current location when this places are populated

        @JvmField
        var searchLatLng: LatLng? = null // Search location for finding this places

        @JvmField
        var mediaList: List<Media> = emptyList() // Search location for finding this places
    }
}
