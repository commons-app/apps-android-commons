package fr.free.nrw.commons.nearby

/**
 * This class groups visual map item Marker with the reated data of displayed place and information
 * of bookmark
 */
class MarkerPlaceGroup(
    // True if user bookmarked the place
    var isBookmarked: Boolean,
    // Place of the location displayed by the marker
    @JvmField val place: Place
)