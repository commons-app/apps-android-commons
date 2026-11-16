package fr.free.nrw.commons.nearby.fragments

import fr.free.nrw.commons.contributions.MainActivity
import fr.free.nrw.commons.contributions.MainActivity.ActiveFragment
import fr.free.nrw.commons.nearby.Place
import org.osmdroid.util.GeoPoint

/**
 * Moves map camera to the specified GeoPoint.
 */
internal fun NearbyParentFragment.moveCameraToPosition(geoPoint: GeoPoint) {
    viewportHelper.moveCameraToPosition(geoPoint)
}

/**
 * Moves map camera to the specified GeoPoint with given zoom level and animation speed.
 */
internal fun NearbyParentFragment.moveCameraToPosition(
    geoPoint: GeoPoint,
    zoom: Double,
    speed: Long,
) {
    viewportHelper.moveCameraToPosition(geoPoint, zoom, speed)
}

/**
 * Updates user location and error overlays on the map.
 */
internal fun NearbyParentFragment.updateUserLocationOverlays(
    geoPoint: GeoPoint?,
    invalidate: Boolean,
) {
    markerOverlayHelper.updateUserLocationOverlays(geoPoint, invalidate)
}

/**
 * Highlights nearest place when user clicks on home nearby banner
 *
 * @param nearestPlace nearest place, which has to be highlighted
 */
internal fun NearbyParentFragment.highlightNearestPlace(nearestPlace: Place) {
    binding?.bottomSheetDetails?.icon?.visibility = android.view.View.VISIBLE
    bottomSheetHelper.passInfoToSheet(nearestPlace)
    bottomSheetHelper.hideBottomSheet()
    bottomSheetHelper.bottomSheetDetailsBehavior?.state =
        com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
}

/**
 * Adds photo marker on map if depicts picker mode is active.
 */
internal fun NearbyParentFragment.addPhotoMarkerIfDepictsMode() {
    markerOverlayHelper.addPhotoMarkerIfDepictsMode()
}

/**
 * Fetch Explore map camera data from fragment arguments if any.
 */
internal fun NearbyParentFragment.loadExploreMapData() {
    // get fragment arguments
    if (arguments != null) {
        prevZoom = requireArguments().getDouble("prev_zoom")
        prevLatitude = requireArguments().getDouble("prev_latitude")
        prevLongitude = requireArguments().getDouble("prev_longitude")
    }
}

/**
 * Checks if fragment arguments contain data from Explore map. if present, then the user
 * navigated from Explore using 'Show in Nearby'.
 *
 * @return true if user navigated from Explore map
 */
internal fun NearbyParentFragment.isCameFromExploreMap(): Boolean =
    prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0

/**
 * Performs map-ready setup actions including permission verification and presenter notification.
 */
internal fun NearbyParentFragment.performMapReadyActions() {
    if (!isDepictsPickerMode &&
        activity is MainActivity &&
        (activity as MainActivity).activeFragment == ActiveFragment.NEARBY
    ) {
        if (applicationKvStore.getBoolean("doNotAskForLocationPermission", false) &&
            locationPermissionsHelper?.checkLocationPermission(requireActivity()) != true
        ) {
            isPermissionDenied = true
        }
    }
    presenter?.onMapReady()
}

/**
 * Handles callback when location permission is granted.
 */
internal fun NearbyParentFragment.locationPermissionGranted() {
    locationHelper.locationPermissionGranted()
}

/**
 * Starts the map without GPS and without permission By default it points to DEFAULT_LATITUDE,DEFAULT_LONGITUDE
 * coordinates, other than that it points to the last known location which can be get by the key
 * "LastLocation" from applicationKvStore
 */
internal fun NearbyParentFragment.startMapWithoutPermission() {
    locationHelper.startMapWithoutPermission()
}
