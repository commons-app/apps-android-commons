package fr.free.nrw.commons.nearby.fragments

import android.app.Activity
import android.content.res.Configuration
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import timber.log.Timber
import kotlin.math.ln

private const val CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT = 0.005
private const val CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE = 0.002
private const val DEFAULT_BOUNDING_BOX_ZOOM = 15.0
private const val DEGREES_IN_FULL_CIRCLE = 360.0
private const val MAP_TILE_SIZE_PX = 256.0
private const val LOG_BASE_TWO = 2.0
private const val DEFAULT_LATITUDE = 51.50550
private const val DEFAULT_LONGITUDE = -0.07520
private const val LOCATION_ACCURACY_METERS = 100f

/**
 * Manages map camera viewport calculations, bounds projection, camera animations,
 * and orientation-aware map centering for NearbyParentFragment.
 */
class NearbyViewportHelper : NearbyParentFragmentContract.Viewport {
    /**
     * Interface defining callbacks for fragment-level actions triggered by viewport events.
     */
    interface NearbyViewportActions {
        /** Invoked to highlight a place on the map and bottom sheet. */
        fun onPlaceHighlighted(place: Place)

        /** Invoked to recenter the map to a target coordinate. */
        fun onRecenterMapRequested(currentLatLng: LatLng?)
    }

    @VisibleForTesting
    var lastMapFocus: GeoPoint? = null
        internal set

    @VisibleForTesting
    var lastPlaceToCenter: Place? = null

    internal var prevZoom: Double = 0.0
    internal var prevLatitude: Double = 0.0
    internal var prevLongitude: Double = 0.0

    var allPhotoLatLngs: ArrayList<LatLng>? = null
        internal set
    var photoPinBoundingBox: BoundingBox? = null
        internal set
    var needsZoomOut: Boolean = true
        internal set
    var nearestPlace: Place? = null
        internal set

    /**
     * Fetch Explore map camera data from fragment arguments if any.
     */
    fun loadExploreMapData(arguments: Bundle?) {
        // get fragment arguments
        if (arguments != null) {
            prevZoom = arguments.getDouble("prev_zoom")
            prevLatitude = arguments.getDouble("prev_latitude")
            prevLongitude = arguments.getDouble("prev_longitude")
        }
    }

    private var mapViewProvider: (() -> MapView?)? = null
    private var activityProvider: (() -> Activity?)? = null
    private var actions: NearbyViewportActions? = null

    /**
     * Initializes helper dependencies and references.
     *
     * @param mapViewProvider Provider for MapView
     * @param activityProvider Provider for Activity
     * @param actions Callbacks for fragment actions
     */
    fun init(
        mapViewProvider: () -> MapView?,
        activityProvider: () -> Activity?,
        actions: NearbyViewportActions,
    ) {
        this.mapViewProvider = mapViewProvider
        this.activityProvider = activityProvider
        this.actions = actions
    }

    /**
     * Returns the target LatLng of the camera, or null if map is uninitialized.
     */
    override fun getCameraTarget(): LatLng? = if (mapViewProvider?.invoke() == null) null else getMapFocus()

    /**
     * Centers the map in nearby fragment to a given place and updates nearestPlace
     *
     * @param place is new center of the map
     */
    override fun centerMapToPlace(place: Place?) {
        Timber.d("Map is centered to place")
        if (null != place) {
            lastPlaceToCenter = place
            actions?.onPlaceHighlighted(place)
        }

        val targetPlace = lastPlaceToCenter
        val activity = activityProvider?.invoke()
        if (targetPlace != null && activity != null) {
            val configuration = activity.resources.configuration
            val cameraShift =
                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    CAMERA_TARGET_SHIFT_FACTOR_PORTRAIT
                } else {
                    CAMERA_TARGET_SHIFT_FACTOR_LANDSCAPE
                }
            actions?.onRecenterMapRequested(
                LatLng(
                    targetPlace.location.latitude - cameraShift,
                    targetPlace.location.longitude,
                    0f,
                ),
            )
        }
    }

    /**
     * Returns the coordinates of the previous map camera focus.
     */
    override fun getLastMapFocus(): LatLng {
        val focus = lastMapFocus ?: GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        return LatLng(
            focus.latitude,
            focus.longitude,
            LOCATION_ACCURACY_METERS,
        )
    }

    /**
     * Returns the coordinates of the current map camera focus.
     */
    override fun getMapFocus(): LatLng {
        val map = mapViewProvider?.invoke()
        val center = map?.mapCenter ?: GeoPoint(DEFAULT_LATITUDE, DEFAULT_LONGITUDE)
        return LatLng(
            center.latitude,
            center.longitude,
            LOCATION_ACCURACY_METERS,
        )
    }

    /**
     * Returns the location of the top right corner of the map view.
     *
     * @return a `LatLng` object denoting the location of the top right corner of the map.
     */
    override fun getScreenTopRight(): LatLng? {
        val map = mapViewProvider?.invoke() ?: return null
        val screenTopRight = map.projection?.fromPixels(map.width, 0)
        return screenTopRight?.let { LatLng(it.latitude, it.longitude, 0f) }
    }

    /**
     * Returns the location of the bottom left corner of the map view.
     *
     * @return a `LatLng` object denoting the location of the bottom left corner of the map.
     */
    override fun getScreenBottomLeft(): LatLng? {
        val map = mapViewProvider?.invoke() ?: return null
        val screenBottomLeft = map.projection?.fromPixels(0, map.height)
        return screenBottomLeft?.let { LatLng(it.latitude, it.longitude, 0f) }
    }

    /**
     * Animates the map camera to the specified GeoPoint.
     *
     * @param geoPoint Target GeoPoint location
     */
    fun moveCameraToPosition(geoPoint: GeoPoint) {
        mapViewProvider?.invoke()?.controller?.animateTo(geoPoint)
    }

    /**
     * Moves the camera of the map view to the specified GeoPoint at specified zoom level and speed
     * using an animation.
     *
     * @param geoPoint The GeoPoint representing the new camera position for the map.
     * @param zoom Zoom level of the map camera
     * @param speed Speed of animation
     */
    fun moveCameraToPosition(
        geoPoint: GeoPoint,
        zoom: Double,
        speed: Long,
    ) {
        mapViewProvider?.invoke()?.controller?.animateTo(geoPoint, zoom, speed)
    }
}

/**
 * Helper method to get the zoom level required to show the bounding box
 */
fun MapView.calculateBoundingBoxZoom(
    boundingBox: BoundingBox,
    padding: Int,
): Double {
    val mapWidth = (width - padding * 2).toDouble()
    val mapHeight = (height - padding * 2).toDouble()
    if (mapWidth <= 0 || mapHeight <= 0) return DEFAULT_BOUNDING_BOX_ZOOM

    val zoomLat =
        ln(mapHeight * DEGREES_IN_FULL_CIRCLE / (MAP_TILE_SIZE_PX * boundingBox.latitudeSpan)) / ln(LOG_BASE_TWO)
    val zoomLng =
        ln(mapWidth * DEGREES_IN_FULL_CIRCLE / (MAP_TILE_SIZE_PX * boundingBox.longitudeSpan)) / ln(LOG_BASE_TWO)

    return minOf(zoomLat, zoomLng)
}

/**
 * Checks if fragment arguments contain data from Explore map. if present, then the user
 * navigated from Explore using 'Show in Nearby'.
 *
 * @return true if user navigated from Explore map
 */
fun NearbyViewportHelper.isCameFromExploreMap(): Boolean =
    prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0

/** Returns the Explore map target camera position as a GeoPoint. */
fun NearbyViewportHelper.getExploreMapGeoPoint(): GeoPoint = GeoPoint(prevLatitude, prevLongitude)

/** Returns the Explore map camera zoom level. */
fun NearbyViewportHelper.getExploreMapZoom(): Double = prevZoom

/**
 * Configures viewport camera and photo coordinates for depicts picker mode.
 */
fun NearbyViewportHelper.setupDepictsPickerMode(photoLatLngs: ArrayList<LatLng>?) {
    allPhotoLatLngs = photoLatLngs
    if (!photoLatLngs.isNullOrEmpty()) {
        val first = photoLatLngs[0]
        // center on average of all photos coordinates and zoom out and run only once
        lastMapFocus = GeoPoint(first.latitude, first.longitude)
        needsZoomOut = true
    }
}

/** Sets the nearest place to the current map focus. */
fun NearbyViewportHelper.setNearestPlace(place: Place?) {
    nearestPlace = place
}

/** Sets the last map focus coordinate. */
fun NearbyViewportHelper.setLastMapFocus(geoPoint: GeoPoint?) {
    lastMapFocus = geoPoint
}

/** Sets the bounding box enclosing depicted photo pins. */
fun NearbyViewportHelper.setPhotoPinBoundingBox(box: BoundingBox) {
    photoPinBoundingBox = box
}

/** Sets whether the map camera needs to zoom out to fit photo pins. */
fun NearbyViewportHelper.setNeedsZoomOut(needsZoom: Boolean) {
    needsZoomOut = needsZoom
}
