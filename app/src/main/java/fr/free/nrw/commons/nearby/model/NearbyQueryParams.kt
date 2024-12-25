package fr.free.nrw.commons.nearby.model

import fr.free.nrw.commons.location.LatLng

sealed class NearbyQueryParams {
    class Rectangular(val screenTopRight: LatLng, val screenBottomLeft: LatLng) :
        NearbyQueryParams()

    class Radial(val center: LatLng, val radiusInKm: Float) : NearbyQueryParams()
}