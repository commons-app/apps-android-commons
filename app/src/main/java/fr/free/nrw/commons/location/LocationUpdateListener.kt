package fr.free.nrw.commons.location

interface LocationUpdateListener {
    // Will be used to update all nearby markers on the map
    fun onLocationChangedSignificantly(latLng: LatLng?)

    // Will be used to track users motion
    fun onLocationChangedSlightly(latLng: LatLng?)

    // Will be used updating nearby card view notification
    fun onLocationChangedMedium(latLng: LatLng?)
}