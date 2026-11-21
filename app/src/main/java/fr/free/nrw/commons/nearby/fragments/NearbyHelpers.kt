package fr.free.nrw.commons.nearby.fragments

/**
 * Encapsulates all helper delegates used by [NearbyParentFragment] to avoid constructor
 * parameter bloating and comply with Detekt's LongParameterList rule.
 */
class NearbyHelpers {
    val fileExportHelper: NearbyFileExportHelper = NearbyFileExportHelper()
    val markerOverlayHelper: NearbyMarkerOverlayHelper = NearbyMarkerOverlayHelper()
    val bottomSheetHelper: NearbyBottomSheetHelper = NearbyBottomSheetHelper()
    val locationHelper: NearbyLocationHelper = NearbyLocationHelper()
    val viewportHelper: NearbyViewportHelper = NearbyViewportHelper()
    val filterHelper: NearbyFilterHelper = NearbyFilterHelper()
    val fabHelper: NearbyFabHelper = NearbyFabHelper()
    val mediaDetailHelper: NearbyMediaDetailHelper = NearbyMediaDetailHelper()
    val placesQueryHelper: NearbyPlacesQueryHelper = NearbyPlacesQueryHelper()
    val statusHelper: NearbyStatusHelper = NearbyStatusHelper()
}
