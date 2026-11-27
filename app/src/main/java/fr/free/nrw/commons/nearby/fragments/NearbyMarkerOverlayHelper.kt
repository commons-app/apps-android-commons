package fr.free.nrw.commons.nearby.fragments

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.NearbyFilterState
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.ExecutorUtils.get
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants.UnitOfMeasure
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import timber.log.Timber
import java.util.concurrent.ExecutorService

private const val SCALE_BAR_OFFSET_X = 15
private const val SCALE_BAR_OFFSET_Y = 25
private const val SCALE_BAR_BACKGROUND_ALPHA = 200
private const val SCALE_BAR_BACKGROUND_RED = 255
private const val SCALE_BAR_BACKGROUND_GREEN = 250
private const val SCALE_BAR_BACKGROUND_BLUE = 250
private const val MARKER_LABEL_FONT_SIZE = 40
private const val MARKER_ANCHOR_V = 0.77525f
private const val MARKER_PRIORITY_PURPLE = 5
private const val MARKER_PRIORITY_RED = 4
private const val MARKER_PRIORITY_MONUMENT = 3
private const val MARKER_PRIORITY_GREEN = 2
private const val MARKER_PRIORITY_GREY = 1
private const val CURRENT_LOCATION_LABEL_FONT_SIZE = 24
private const val CURRENT_LOCATION_ANCHOR = 0.5f
private const val LOCATION_CIRCLE_GRAY = 128
private const val LOCATION_CIRCLE_FILL_ALPHA = 40
private const val ACCURACY_CONFIDENCE_MULTIPLIER = 2
private const val ERROR_STROKE_WIDTH = 2f

/**
 * Manages map markers, custom icons, marker priority, and user location overlays on the Nearby map.
 */
class NearbyMarkerOverlayHelper(
    private val executor: ExecutorService = get(),
) : NearbyParentFragmentContract.MarkerOverlay {
    /**
     * Interface defining callbacks for marker click events and camera actions.
     */
    interface MarkerCallback {
        /**
         * Invoked when a place marker is clicked.
         *
         * @param place The place associated with the clicked marker
         * @param marker The marker instance that was clicked
         * @param isBookmarked True if the place is bookmarked by the user
         */
        fun onMarkerClicked(
            place: Place,
            marker: Marker,
            isBookmarked: Boolean,
        )

        /**
         * Invoked when a photo marker in depicts mode is clicked.
         *
         * @param marker The photo marker that was clicked
         */
        fun onPhotoMarkerClicked(marker: Marker)

        /**
         * Invoked when a bounding box encompassing all photo markers is calculated.
         *
         * @param boundingBox The bounding box containing all photo coordinates
         */
        fun onBoundingBoxZoomCalculated(boundingBox: BoundingBox)
    }

    /**
     * Provider interface supplying dynamic map and location state from the host fragment.
     */
    interface MapStateProvider {
        /** Returns the current map center geo point. */
        fun getMapCenter(): GeoPoint?

        /** Returns the map events overlay instance. */
        fun getMapEventsOverlay(): MapEventsOverlay?

        /** Returns true if location permission has been denied. */
        fun isPermissionDenied(): Boolean

        /** Returns true if the fragment is in depicts picker mode. */
        fun isDepictsPickerMode(): Boolean

        /** Returns photo coordinates in depicts picker mode. */
        fun getAllPhotoLatLngs(): List<LatLng>?

        /** Returns the nearest place if highlighted. */
        fun getNearestPlace(): Place?
    }

    @VisibleForTesting
    var clickedMarker: Marker? = null

    @VisibleForTesting
    var userLocationOverlay: Overlay? = null

    @VisibleForTesting
    var userLocationErrorOverlay: Overlay? = null

    @VisibleForTesting
    var photoPinBoundingBox: BoundingBox? = null

    @VisibleForTesting
    val drawableCache: MutableMap<Pair<Context, Int>, Drawable> = HashMap()

    private var mapViewProvider: (() -> MapView?)? = null
    private var contextProvider: (() -> Context?)? = null
    private var locationManagerProvider: (() -> LocationServiceManager?)? = null
    private var mapStateProvider: MapStateProvider? = null
    private var markerCallback: MarkerCallback? = null

    /**
     * Initializes helper dependencies and references.
     *
     * @param mapViewProvider Provider for the MapView
     * @param contextProvider Provider for Context
     * @param locationManagerProvider Provider for LocationServiceManager
     * @param mapStateProvider Provider supplying dynamic map and location state
     * @param markerCallback Callback for marker clicks and viewport updates
     */
    fun init(
        mapViewProvider: () -> MapView?,
        contextProvider: () -> Context?,
        locationManagerProvider: () -> LocationServiceManager?,
        mapStateProvider: MapStateProvider,
        markerCallback: MarkerCallback,
    ) {
        this.mapViewProvider = mapViewProvider
        this.contextProvider = contextProvider
        this.locationManagerProvider = locationManagerProvider
        this.mapStateProvider = mapStateProvider
        this.markerCallback = markerCallback
    }

    /**
     * Adds a marker for the user's current position. Adds a circle which uses the accuracy * 2, to
     * draw a circle which represents the user's position with an accuracy of 95%.
     *
     *
     * Should be called only on creation of Map, there is other method to update markers location
     * with users move.
     *
     * @param currentLatLng current location
     */
    override fun addCurrentLocationMarker(currentLatLng: LatLng?) {
        val locationManager = locationManagerProvider?.invoke()
        val isDenied = mapStateProvider?.isPermissionDenied() ?: false
        if (null != currentLatLng &&
            !isDenied &&
            locationManager?.isGPSProviderEnabled() == true
        ) {
            executor.submit {
                Timber.d("Adds current location marker")
                recenterMarkerToPosition(
                    GeoPoint(currentLatLng.latitude, currentLatLng.longitude),
                )
            }
        } else {
            Timber.d("not adding current location marker..current location is null")
        }
    }

    /**
     * Clears all place markers from the map.
     */
    override fun filterOutAllMarkers() {
        clearAllMarkers()
    }

    /**
     * Filters markers based on selectedLabels and chips
     *
     * @param selectedLabels       label list that user clicked
     * @param filterForPlaceState  true if we filter places for place state
     * @param filterForAllNoneType true if we filter places with all none button
     */
    override fun filterMarkersByLabels(
        selectedLabels: List<Label>?,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean,
    ) {
        val onlyNeedsPhoto = isOnlyNeedsPhoto()
        if (selectedLabels == null || selectedLabels.isEmpty()) {
            replaceMarkerOverlays(
                NearbyParentFragment.placesToShow(NearbyController.markerLabelList, onlyNeedsPhoto),
            )
            return
        }
        val placeGroupsToShow =
            NearbyController.markerLabelList.filter { markerPlaceGroup ->
                shouldDisplayMarker(markerPlaceGroup, selectedLabels, filterForPlaceState)
            }
        replaceMarkerOverlays(NearbyParentFragment.placesToShow(placeGroupsToShow, onlyNeedsPhoto))
    }

    override fun isOnlyNeedsPhoto(): Boolean =
        NearbyFilterState.getInstance().isNeedPhotoSelected && !(mapStateProvider?.isDepictsPickerMode() ?: false)

    /**
     * Adds multiple markers representing places to the map and handles item gestures.
     * Markers are sorted by priority before being added so that higher-priority pins
     * (e.g. red "needs photo" pins) are always rendered on top of lower-priority ones
     * (e.g. green "has photo" pins) in the OSMDroid overlay list.
     *
     * @param markerPlaceGroups The list of marker place groups containing the places and
     * their bookmarked status
     */
    override fun replaceMarkerOverlays(markerPlaceGroups: List<MarkerPlaceGroup>) {
        val mapView = mapViewProvider?.invoke() ?: return
        val context = contextProvider?.invoke() ?: return
        val nearestPlace = mapStateProvider?.getNearestPlace()

        // Reverse the list first to make it farthest-first (since it comes nearest-first).
        // Then sort by priority (stable sort). This ensures that within the same priority,
        // farthest markers are added first (drawn bottom) and nearest are added last (drawn top).
        val sortedGroups =
            markerPlaceGroups.reversed().sortedBy {
                getMarkerPriority(it.place, it.isBookmarked, nearestPlace)
            }
        val markerContext =
            MarkerContext(
                context = context,
                mapView = mapView,
                nearestPlace = nearestPlace,
                cache = drawableCache,
                onMarkerClicked = { p, m, b ->
                    clickedMarker?.closeInfoWindow()
                    clickedMarker = m
                    markerCallback?.onMarkerClicked(p, m, b)
                },
            )
        val newMarkers =
            sortedGroups.map { group ->
                convertToMarker(group.place, group.isBookmarked, markerContext)
            }
        clearAllMarkers()
        mapView.overlays.addAll(newMarkers)
    }

    /**
     * Clears all markers from the map and resets certain map overlays and gestures. After clearing
     * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
     */
    override fun clearAllMarkers() {
        val mapView = mapViewProvider?.invoke() ?: return
        mapView.overlayManager.clear()

        var geoPoint = mapStateProvider?.getMapCenter()
        val lastLatLng = locationManagerProvider?.invoke()?.getLastLocation()
        if (lastLatLng != null) {
            geoPoint = GeoPoint(lastLatLng.latitude, lastLatLng.longitude)
        }
        updateUserLocationOverlays(geoPoint, false)

        val scaleBarOverlay =
            ScaleBarOverlay(mapView).apply {
                setScaleBarOffset(SCALE_BAR_OFFSET_X, SCALE_BAR_OFFSET_Y)
                val barPaint =
                    Paint().apply {
                        setARGB(
                            SCALE_BAR_BACKGROUND_ALPHA,
                            SCALE_BAR_BACKGROUND_RED,
                            SCALE_BAR_BACKGROUND_GREEN,
                            SCALE_BAR_BACKGROUND_BLUE,
                        )
                    }
                setBackgroundPaint(barPaint)
                enableScaleBar()
            }
        mapView.overlays.add(scaleBarOverlay)

        val mapEventsOverlay = mapStateProvider?.getMapEventsOverlay()
        if (mapEventsOverlay != null) {
            mapView.overlays.add(mapEventsOverlay)
        }

        if (mapStateProvider?.isDepictsPickerMode() == true) {
            val coords = mapStateProvider?.getAllPhotoLatLngs()
            if (!coords.isNullOrEmpty()) {
                addPhotoMarkers(coords)
            }
        }
        mapView.setMultiTouchControls(true)
        mapView.invalidate()
    }

    /**
     * Recenters the map to the Center and adds a scale disk overlay and a marker at the position.
     *
     * @param geoPoint The GeoPoint representing the new center position of the map.
     */
    fun recenterMarkerToPosition(geoPoint: GeoPoint?) {
        val mapView = mapViewProvider?.invoke()
        geoPoint?.let {
            mapView?.controller?.setCenter(it)
            updateUserLocationOverlays(it, true)
        }
    }

    /**
     * Updates the user current location overlays (both the location and error overlays) by
     * replacing any existing location overlays with new overlays at the given GeoPoint. If there
     * are no existing location and error overlays, then new overlays are added.
     *
     * @param geoPoint The GeoPoint representing the user's current location.
     * @param invalidate If true, the map overlays will be invalidated after the user
     * location overlays are updated/added. If false, the overlays will not be invalidated.
     */
    fun updateUserLocationOverlays(
        geoPoint: GeoPoint?,
        invalidate: Boolean,
    ) {
        val mapView = mapViewProvider?.invoke()
        val context = contextProvider?.invoke()
        val locationManager = locationManagerProvider?.invoke()

        geoPoint?.let {
            userLocationOverlay =
                updateUserLocationOverlay(
                    mapView,
                    it,
                    context,
                    userLocationOverlay,
                    drawableCache,
                )
            userLocationErrorOverlay =
                updateUserLocationErrorOverlay(
                    mapView,
                    it,
                    context,
                    locationManager,
                    userLocationErrorOverlay,
                )
        }

        if (invalidate) {
            mapView?.invalidate()
        }
    }

    /**
     * Adds photo markers representing coordinates to the map and calculates their bounding box.
     *
     * @param coordList List of photo coordinates
     */
    fun addPhotoMarkers(coordList: List<LatLng>) {
        val mapView = mapViewProvider?.invoke()
        val context = contextProvider?.invoke()
        if (mapView == null || context == null || coordList.isEmpty()) {
            return
        }

        coordList.forEachIndexed { index, latLng ->
            // add marker details
            val marker = Marker(mapView)
            marker.position = GeoPoint(latLng.latitude, latLng.longitude)
            marker.title = "Photo ${index + 1}"
            marker.icon = getCachedDrawable(context, R.drawable.ic_mappin, drawableCache)
            mapView.overlays.add(marker)
            // disable zooming in to the pin , only show the info
            // close details of if open place to avoid confusion
            marker.setOnMarkerClickListener { m: Marker, _ ->
                markerCallback?.onPhotoMarkerClicked(m)
                m.showInfoWindow()
                true
            }
        }

        var minLat = Double.MAX_VALUE
        var maxLat = -Double.MAX_VALUE
        var minLng = Double.MAX_VALUE
        var maxLng = -Double.MAX_VALUE

        val allPhotoLatLngs = mapStateProvider?.getAllPhotoLatLngs() ?: coordList
        allPhotoLatLngs.forEach {
            minLat = minLat.coerceAtMost(it.latitude)
            maxLat = maxLat.coerceAtLeast(it.latitude)
            minLng = minLng.coerceAtMost(it.longitude)
            maxLng = maxLng.coerceAtLeast(it.longitude)
        }
        val boundingBox = BoundingBox(maxLat, maxLng, minLat, minLng)
        photoPinBoundingBox = boundingBox
        markerCallback?.onBoundingBoxZoomCalculated(boundingBox)
        mapView.invalidate()
    }
}

internal data class MarkerContext(
    val context: Context?,
    val mapView: MapView?,
    val nearestPlace: Place?,
    val cache: MutableMap<Pair<Context, Int>, Drawable>?,
    val onMarkerClicked: ((Place, Marker, Boolean) -> Unit)?,
)

private fun shouldDisplayMarker(
    markerPlaceGroup: MarkerPlaceGroup,
    selectedLabels: List<Label>,
    filterForPlaceState: Boolean,
): Boolean {
    val place = markerPlaceGroup.place
    // When label filter is engaged
    // then compare it against place's label
    val labelFilterEngaged = selectedLabels.isNotEmpty() || !filterForPlaceState
    val matchesPlaceLabel = selectedLabels.contains(place.label)
    val matchesBookmark = selectedLabels.contains(Label.BOOKMARKS) && markerPlaceGroup.isBookmarked
    val displayWlm = false

    val isExcludedByLabel = labelFilterEngaged && !matchesPlaceLabel && !matchesBookmark
    val isExcludedByMonument = !displayWlm && place.isMonument

    // Exists and needs photo
    // Exists and all included needs and doesn't needs photo
    // All and only needs photo
    // all
    return !(isExcludedByLabel || isExcludedByMonument)
}

/**
 * Updates the user location overlay by either replacing it with or adding a new one.
 *
 * If the user location overlay is null, the new overlay is added. If the
 * overlay is not null, it is replaced by the new overlay.
 *
 * @param mapView The MapView instance
 * @param geoPoint The GeoPoint representing the user's location
 * @param context The Android context
 * @param currentOverlay Current location overlay if previously added
 * @param cache Optional drawable cache
 * @return The updated user location overlay
 */
private fun updateUserLocationOverlay(
    mapView: MapView?,
    geoPoint: GeoPoint,
    context: Context?,
    currentOverlay: Overlay?,
    cache: MutableMap<Pair<Context, Int>, Drawable>?,
): Overlay? {
    if (mapView == null || context == null) {
        return null
    }
    val overlays = mapView.overlays
    val overlay = createCurrentLocationOverlay(mapView, context, geoPoint, cache)
    val index = overlays.indexOf(currentOverlay)

    if (currentOverlay == null || index == -1) {
        overlays.add(overlay)
    } else {
        overlays[index] = overlay
    }
    return overlay
}

/**
 * @return The accuracy of the current location with a confidence at the 68th percentile.
 * Units are in meters. Returning 0 may indicate failure.
 */
private fun getCurrentLocationAccuracy(locationManager: LocationServiceManager?): Float {
    var accuracy = 0f
    val lastLocation = locationManager?.getLastLocation()
    if (lastLocation != null) {
        accuracy = lastLocation.accuracy
    }
    return accuracy
}

/**
 * Updates the user location error overlay by either replacing it with or adding a new one.
 *
 * If the user location error overlay is null, the new overlay is added. If the
 * overlay is not null, it is replaced by the new overlay.
 *
 * @param mapView The MapView instance
 * @param geoPoint The GeoPoint representing the user's location
 * @param context The Android context
 * @param locationManager LocationServiceManager providing GPS accuracy
 * @param currentErrorOverlay Current location error overlay if previously added
 * @return The updated user location error overlay
 */
private fun updateUserLocationErrorOverlay(
    mapView: MapView?,
    geoPoint: GeoPoint,
    context: Context?,
    locationManager: LocationServiceManager?,
    currentErrorOverlay: Overlay?,
): Overlay? {
    val overlays = mapView?.overlays ?: return null
    // Multiply accuracy by 2 to get 95% confidence interval
    val accuracy = getCurrentLocationAccuracy(locationManager) * ACCURACY_CONFIDENCE_MULTIPLIER
    val overlay =
        createCurrentLocationErrorOverlay(
            context,
            geoPoint,
            accuracy.toInt(),
            UnitOfMeasure.meter,
        )

    val index = overlays.indexOf(currentErrorOverlay)

    if (currentErrorOverlay == null || index == -1) {
        overlays.add(overlay)
    } else {
        overlays[index] = overlay
    }
    return overlay
}

/**
 * Returns drawable of marker icon for given place
 *
 * @param place        where marker is to be added
 * @param isBookmarked true if place is bookmarked
 * @param nearestPlace place that is currently highlighted as nearest
 * @return returns the drawable of marker according to the place information
 */
internal fun getIconFor(
    place: Place,
    isBookmarked: Boolean,
    nearestPlace: Place? = null,
): Int =
    when {
        nearestPlace != null && place.name == nearestPlace.name -> {
            // Highlight nearest place only when user clicks on the home nearby banner
            // highlightNearestPlace(place);
            if (isBookmarked) {
                R.drawable.ic_custom_map_marker_purple_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_purple
            }
        }
        place.isMonument -> R.drawable.ic_custom_map_marker_monuments
        place.pic?.trim { it <= ' ' }?.isNotEmpty() == true -> {
            if (isBookmarked) {
                R.drawable.ic_custom_map_marker_green_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_green
            }
        }
        !place.exists -> {
            // Means that the topic of the Wikidata item does not exist in the real world anymore,
            // for instance it is a past event, or a place that was destroyed
            R.drawable.ic_clear_black_24dp
        }
        place.name.isNullOrEmpty() -> {
            if (isBookmarked) {
                R.drawable.ic_custom_map_marker_grey_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_grey
            }
        }
        else -> {
            if (isBookmarked) {
                R.drawable.ic_custom_map_marker_red_bookmarked
            } else {
                R.drawable.ic_custom_map_marker_red
            }
        }
    }

/**
 * Returns a rendering priority for a marker derived from [getIconFor], where a higher value
 * means the marker is drawn on top (OSMDroid renders overlays last-in = on top).
 * All place-type logic stays in [getIconFor]; this method only maps icon → priority.
 *
 *  5 – Purple  : nearest / highlighted place
 *  4 – Red     : exists, needs a photo  ← must always be above green
 *  3 – Monument: WLM monument markers
 *  2 – Green   : already has a photo
 *  1 – Grey    : loading (no name yet)
 *  0 – Clear   : does not exist in the real world
 */
internal fun getMarkerPriority(
    place: Place,
    isBookmarked: Boolean,
    nearestPlace: Place? = null,
): Int =
    when (getIconFor(place, isBookmarked, nearestPlace)) {
        R.drawable.ic_custom_map_marker_purple,
        R.drawable.ic_custom_map_marker_purple_bookmarked,
        -> MARKER_PRIORITY_PURPLE
        R.drawable.ic_custom_map_marker_red,
        R.drawable.ic_custom_map_marker_red_bookmarked,
        -> MARKER_PRIORITY_RED
        R.drawable.ic_custom_map_marker_monuments -> MARKER_PRIORITY_MONUMENT
        R.drawable.ic_custom_map_marker_green,
        R.drawable.ic_custom_map_marker_green_bookmarked,
        -> MARKER_PRIORITY_GREEN
        R.drawable.ic_custom_map_marker_grey,
        R.drawable.ic_custom_map_marker_grey_bookmarked,
        -> MARKER_PRIORITY_GREY
        else -> 0 // ic_clear_black_24dp – place does not exist in the real world
    }

/**
 * Creates the current location overlay
 *
 * @param mapView The MapView to which the marker will be added
 * @param context The Android context
 * @param geoPoint The GeoPoint where the current location overlay will be placed.
 * @param cache Optional drawable cache
 * @return The current location overlay as a Marker
 */
internal fun createCurrentLocationOverlay(
    mapView: MapView,
    context: Context,
    geoPoint: GeoPoint,
    cache: MutableMap<Pair<Context, Int>, Drawable>? = null,
): Marker {
    val currentLocationOverlay = Marker(mapView)
    currentLocationOverlay.position = geoPoint
    currentLocationOverlay.icon = getCachedDrawable(context, R.drawable.current_location_marker, cache)
    currentLocationOverlay.title = "Your Location"
    currentLocationOverlay.textLabelFontSize = CURRENT_LOCATION_LABEL_FONT_SIZE
    currentLocationOverlay.setAnchor(CURRENT_LOCATION_ANCHOR, CURRENT_LOCATION_ANCHOR)
    return currentLocationOverlay
}

/**
 * Creates the location error overlay to show the user how accurate the current location
 * overlay is. The edge of the disk is the 95% confidence interval.
 *
 * @param context The Android context
 * @param point The user's location as a GeoPoint
 * @param value The radius of the disk
 * @param unitOfMeasure The unit of measurement of the value/disk radius.
 *
 * @return The location error overlay as a ScaleDiskOverlay.
 */
internal fun createCurrentLocationErrorOverlay(
    context: Context?,
    point: GeoPoint,
    value: Int,
    unitOfMeasure: UnitOfMeasure,
): ScaleDiskOverlay {
    val scaleDisk = ScaleDiskOverlay(context, point, value, unitOfMeasure)

    scaleDisk.setCirclePaint2(
        Paint().apply {
            color = Color.rgb(LOCATION_CIRCLE_GRAY, LOCATION_CIRCLE_GRAY, LOCATION_CIRCLE_GRAY)
            style = Paint.Style.STROKE
            strokeWidth = ERROR_STROKE_WIDTH
        },
    )

    scaleDisk.setCirclePaint1(
        Paint().apply {
            color =
                Color.argb(
                    LOCATION_CIRCLE_FILL_ALPHA,
                    LOCATION_CIRCLE_GRAY,
                    LOCATION_CIRCLE_GRAY,
                    LOCATION_CIRCLE_GRAY,
                )
            style = Paint.Style.FILL_AND_STROKE
        },
    )

    return scaleDisk
}

/**
 * Converts a [Place] into an OSMDroid [Marker] configured with custom icon, snippet, and click handler.
 *
 * @param place The place domain model
 * @param isBookMarked True if the place is bookmarked
 * @param markerContext Context dependencies and click callback
 * @return Configured Marker instance
 */
internal fun convertToMarker(
    place: Place,
    isBookMarked: Boolean,
    markerContext: MarkerContext,
): Marker {
    val icon =
        getCachedDrawable(
            markerContext.context,
            getIconFor(place, isBookMarked, markerContext.nearestPlace),
            markerContext.cache,
        )
    val lat = place.location?.latitude ?: 0.0
    val lng = place.location?.longitude ?: 0.0
    val point = GeoPoint(lat, lng)
    val marker = Marker(markerContext.mapView)
    marker.position = point
    marker.icon = icon
    if (!place.name.isNullOrEmpty()) {
        marker.title = place.name
        val desc = place.longDescription
        if (desc != null) {
            marker.snippet =
                if (NearbyParentFragment.containsParentheses(desc)) {
                    NearbyParentFragment.getTextBetweenParentheses(desc)
                } else {
                    desc
                }
        }
    }
    marker.textLabelFontSize = MARKER_LABEL_FONT_SIZE
    // anchorV is 21.707/28.0 as icon height is 28dp while the pin base is at 21.707dp from top
    marker.setAnchor(Marker.ANCHOR_CENTER, MARKER_ANCHOR_V)
    marker.setOnMarkerClickListener { marker1: Marker, _ ->
        markerContext.onMarkerClicked?.invoke(place, marker1, isBookMarked)
        true
    }
    return marker
}

/**
 * Gets the specified Drawable object. This is a wrapper method for ContextCompat.getDrawable().
 * This method caches results from previous calls for faster retrieval.
 *
 * @param context The context to use to get the Drawable
 * @param id The integer that describes the Drawable resource
 * @param cache The map caching previously loaded drawables
 * @return The Drawable object
 */
internal fun getCachedDrawable(
    context: Context?,
    id: Int?,
    cache: MutableMap<Pair<Context, Int>, Drawable>?,
): Drawable? {
    if (context == null || id == null) {
        return null
    }

    val key = Pair(context, id)
    val cached = cache?.get(key)
    return if (cached != null) {
        cached
    } else {
        val drawable = ContextCompat.getDrawable(context, id)
        if (drawable != null && cache != null) {
            cache[key] = drawable
        }
        drawable
    }
}
