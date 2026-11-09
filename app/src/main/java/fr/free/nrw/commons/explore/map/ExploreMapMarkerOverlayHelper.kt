package fr.free.nrw.commons.explore.map

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Build
import android.text.Html
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Place
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay.OnItemGestureListener
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import timber.log.Timber
import javax.inject.Inject

private const val SCALE_DISK_RADIUS_FEET = 2000
private const val SCALE_DISK_MIN_SIZE = 900
private const val SCALE_DISK_MAX_SIZE = 1700
private const val SCALE_DISK_GRAY = 128
private const val SCALE_DISK_ALPHA = 40
private const val LOCATION_MARKER_FONT_SIZE = 24
private const val SCALE_BAR_OFFSET_X = 15
private const val SCALE_BAR_OFFSET_Y = 25
private const val SCALE_BAR_ALPHA = 200
private const val SCALE_BAR_COLOR_R = 255
private const val SCALE_BAR_COLOR_G = 250
private const val SCALE_BAR_COLOR_B = 250
private const val MAX_TITLE_LENGTH = 43
private const val TRUNCATED_TITLE_LENGTH = 40
private const val FILE_PREFIX_LENGTH = 5

/**
 * Manages map markers, overlays, and gesture callbacks on the Explore map.
 */
class ExploreMapMarkerOverlayHelper
    @Inject
    constructor() : ExploreMapContract.MarkerOverlay {
        @VisibleForTesting
        var clickedMarker: BaseMarker? = null

        @VisibleForTesting
        var baseMarkerOverlayMap: MutableMap<BaseMarker?, Overlay?>? = null

        @VisibleForTesting
        var mapView: MapView? = null

        var getContext: (() -> Context?)? = null
        var isAttached: (() -> Boolean)? = null
        var detailsSheetHelper: ExploreMapDetailsSheetHelper? = null
        var onMarkerClicked: ((Place) -> Unit)? = null
        var mediaListProvider: (() -> List<Media>?)? = null

        /**
         * Initializes helper dependencies and references.
         *
         * @param mapView MapView instance on which overlays are managed
         * @param getContext Provider for the active context
         * @param isAttached Provider checking whether fragment is attached
         * @param detailsSheetHelper Bottom sheet helper for state synchronization
         * @param onMarkerClicked Callback invoked when a marker is tapped
         */
        fun init(
            mapView: MapView,
            getContext: () -> Context?,
            isAttached: () -> Boolean,
            detailsSheetHelper: ExploreMapDetailsSheetHelper,
            onMarkerClicked: (Place) -> Unit,
        ) {
            this.mapView = mapView
            this.getContext = getContext
            this.isAttached = isAttached
            this.detailsSheetHelper = detailsSheetHelper
            this.onMarkerClicked = onMarkerClicked
        }

        /**
         * Adds a markers to the map based on the list of NearbyBaseMarker.
         *
         * @param nearbyBaseMarkers The NearbyBaseMarker object representing the markers to be added.
         */
        override fun addMarkersToMap(nearbyBaseMarkers: List<BaseMarker?>?) {
            clearAllMarkers()
            val mediaList = mediaListProvider?.invoke()
            nearbyBaseMarkers?.forEach {
                if (it != null) {
                    addMarkerToMap(it, mediaList)
                }
            }
            mapView?.invalidate()
        }

        /**
         * Adds a marker to the map based on the specified NearbyBaseMarker.
         *
         * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be added.
         * @param mediaList Media list used for image matching
         */
        fun addMarkerToMap(
            nearbyBaseMarker: BaseMarker,
            mediaList: List<Media>?,
        ) {
            val context = getContext?.invoke() ?: return
            if (isAttached?.invoke() != true) return

            val items = mutableListOf<OverlayItem?>()
            val d: Drawable = nearbyBaseMarker.icon!!.toDrawable(context.resources)
            val point =
                GeoPoint(
                    nearbyBaseMarker.place.location.latitude,
                    nearbyBaseMarker.place.location.longitude,
                )

            val markerMedia = getMediaFromImageURL(nearbyBaseMarker.place.pic, mediaList)
            var authorUser: String? = null
            if (markerMedia != null) {
                // HTML text is sometimes part of the author string and needs to be removed
                authorUser =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        Html.fromHtml(markerMedia.getAuthorOrUser(), Html.FROM_HTML_MODE_LEGACY)
                    } else {
                        Html.fromHtml(markerMedia.getAuthorOrUser())
                    }.toString()
            }

            var title = nearbyBaseMarker.place.name
            // Remove "File:" if present at start
            if (title.startsWith("File:")) {
                title = title.substring(FILE_PREFIX_LENGTH)
            }
            // Remove extensions like .jpg, .jpeg, .png, .svg (case insensitive)
            title = title.replace("(?i)\\.(jpg|jpeg|png|svg)$".toRegex(), "")
            title = title.replace("_", " ")
            // Truncate if too long because it doesn't fit the screen
            if (title.length > MAX_TITLE_LENGTH) {
                title = title.substring(0, TRUNCATED_TITLE_LENGTH) + "…"
            }

            val item = OverlayItem(title, authorUser, point)
            item.setMarker(d)
            items.add(item)
            val overlay =
                ItemizedOverlayWithFocus<OverlayItem?>(
                    items,
                    object : OnItemGestureListener<OverlayItem?> {
                        override fun onItemSingleTapUp(
                            index: Int,
                            item: OverlayItem?,
                        ): Boolean {
                            val place = nearbyBaseMarker.place
                            if (clickedMarker != null) {
                                removeMarker(clickedMarker)
                                addMarkerToMap(clickedMarker!!, mediaList)
                                detailsSheetHelper?.showDetails()
                            }
                            clickedMarker = nearbyBaseMarker
                            onMarkerClicked?.invoke(place)

                            // Move the overlay to the top so it can be fully seen.
                            val topOverlay = getOverlay(item)
                            val mapOverlays = mapView?.overlays
                            if (topOverlay != null && mapOverlays != null && mapOverlays.remove(topOverlay)) {
                                mapOverlays.add(topOverlay)
                            }
                            return true
                        }

                        override fun onItemLongPress(
                            index: Int,
                            item: OverlayItem?,
                        ): Boolean = false
                    },
                    context,
                )

            if (baseMarkerOverlayMap == null) {
                baseMarkerOverlayMap = HashMap<BaseMarker?, Overlay?>()
            }
            baseMarkerOverlayMap!!.put(nearbyBaseMarker, overlay)

            overlay.setFocusItemsOnTap(true)
            mapView?.overlays?.add(overlay) // Add the overlay to the map
        }

        /**
         * Clears all markers from the map and resets certain map overlays and gestures. After clearing
         * markers, it re-adds a scale bar overlay and rotation gesture overlay to the map.
         */
        override fun clearAllMarkers() {
            val context = getContext?.invoke()
            val map = mapView
            if (isAttached?.invoke() != true || context == null || map == null) {
                return
            }

            map.overlayManager.clear()
            val mapCenter = map.mapCenter as? GeoPoint
            val mediaList = mediaListProvider?.invoke()

            if (mapCenter != null) {
                map.overlays.add(createScaleDiskOverlay(context, mapCenter))
                map.overlays.add(
                    Marker(map).apply {
                        setPosition(mapCenter)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.current_location_marker)
                        title = "Your Location"
                        textLabelFontSize = LOCATION_MARKER_FONT_SIZE
                    },
                )
            }

            map.overlays.add(
                ScaleBarOverlay(map).apply {
                    setScaleBarOffset(SCALE_BAR_OFFSET_X, SCALE_BAR_OFFSET_Y)
                    setBackgroundPaint(
                        Paint().apply {
                            setARGB(SCALE_BAR_ALPHA, SCALE_BAR_COLOR_R, SCALE_BAR_COLOR_G, SCALE_BAR_COLOR_B)
                        },
                    )
                    enableScaleBar()
                },
            )

            map.overlays.add(
                MapEventsOverlay(
                    object : MapEventsReceiver {
                        override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                            handleMapTap(mediaList)
                            return true
                        }

                        override fun longPressHelper(p: GeoPoint?): Boolean = false
                    },
                ),
            )
            map.setMultiTouchControls(true)
        }

        /**
         * Handles map single tap events to reset previously clicked markers and dismiss bottom sheet.
         *
         * @param mediaList Media collection for restoring marker icons
         */
        fun handleMapTap(mediaList: List<Media>?) {
            if (clickedMarker != null) {
                removeMarker(clickedMarker)
                addMarkerToMap(clickedMarker!!, mediaList)
                mapView?.invalidate()
            } else {
                Timber.e("CLICKED MARKER IS NULL")
            }
            // Back should first hide the bottom sheet if it is expanded
            detailsSheetHelper?.handleMapTap()
        }

        /**
         * Removes a marker from the map based on the specified NearbyBaseMarker.
         *
         * @param nearbyBaseMarker The NearbyBaseMarker object representing the marker to be removed.
         */
        fun removeMarker(nearbyBaseMarker: BaseMarker?) {
            val placeName = nearbyBaseMarker?.place?.name
            val map = baseMarkerOverlayMap
            if (placeName == null || map == null || !map.containsKey(nearbyBaseMarker)) {
                return
            }

            val target = map[nearbyBaseMarker]
            val overlays = mapView?.overlays
            if (overlays != null && target != null) {
                val index = overlays.indexOf(target)
                if (index >= 0) {
                    overlays.removeAt(index)
                    mapView?.invalidate()
                    map.remove(nearbyBaseMarker)
                }
            }
        }

        /**
         * Performs a linear search for the first Overlay which contains the specified OverlayItem.
         *
         * @param item The OverlayItem contained within the first target Overlay.
         * @return The first Overlay which contains the specified OverlayItem or null if the Overlay
         * could not be found.
         */
        fun getOverlay(item: OverlayItem?): Overlay? {
            if (item == null) {
                return null
            }
            return mapView
                ?.overlays
                ?.filterIsInstance<ItemizedOverlayWithFocus<*>>()
                ?.firstOrNull { overlay ->
                    (0 until overlay.size()).any { j -> overlay.getItem(j) === item }
                }
        }

        /**
         * Retrieves the specific Media object from the mediaList field.
         *
         * @param url The specific Media's image URL.
         * @param mediaList The list of Media items to search within.
         * @return The Media object that matches the URL or null if it could not be found.
         */
        fun getMediaFromImageURL(
            url: String?,
            mediaList: List<Media>?,
        ): Media? {
            if (mediaList == null || url == null) {
                return null
            }
            return mediaList.firstOrNull { it.imageUrl == url }
        }

        /**
         * Recenters the map view to the specified GeoPoint and updates the marker to indicate the new
         * position.
         *
         * @param geoPoint The GeoPoint representing the new center position for the map.
         */
        fun recenterMarkerToPosition(geoPoint: GeoPoint?) {
            val context = getContext?.invoke() ?: return
            val map = mapView ?: return
            if (geoPoint != null) {
                map.controller.setCenter(geoPoint)
                val overlays = map.overlays
                // collects the indices of items to remove
                val indicesToRemove = mutableListOf<Int>()
                for (i in overlays.indices) {
                    if (overlays[i] is Marker || overlays[i] is ScaleDiskOverlay) {
                        indicesToRemove.add(i)
                    }
                }
                // removes the items in reverse order to avoid index shifting
                indicesToRemove.sortedDescending().forEach { index ->
                    map.overlays.removeAt(index)
                }
                map.overlays.add(createScaleDiskOverlay(context, geoPoint))
                val startMarker =
                    Marker(map).apply {
                        setPosition(geoPoint)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.current_location_marker)
                        title = "Your Location"
                        textLabelFontSize = LOCATION_MARKER_FONT_SIZE
                    }
                map.overlays.add(startMarker)
            }
        }

        /**
         * Draws the current user location marker on the map.
         *
         * @param location Last known user location
         */
        fun drawMyLocationMarker(location: LatLng?) {
            val context = getContext?.invoke() ?: return
            val map = mapView ?: return
            if (location != null) {
                val geoPoint = GeoPoint(location.latitude, location.longitude)
                val startMarker =
                    Marker(map).apply {
                        setPosition(geoPoint)
                        setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                        icon = ContextCompat.getDrawable(context, R.drawable.current_location_marker)
                        title = "Your Location"
                        textLabelFontSize = LOCATION_MARKER_FONT_SIZE
                    }
                map.overlays.add(startMarker)
                map.invalidate()
            }
        }
    }

/**
 * Attaches the marker overlay helper to the ExploreMapFragment view binding.
 *
 * @param binding Layout binding containing the MapView
 * @param fragment The hosting ExploreMapFragment
 */
fun ExploreMapMarkerOverlayHelper.attachMarkerOverlay(
    binding: FragmentExploreMapBinding,
    fragment: ExploreMapFragment,
) {
    mapView = binding.mapView
    getContext = { fragment.context }
    isAttached = { fragment.isVisible && fragment.activity != null }
    detailsSheetHelper = fragment.detailsSheetHelper
    onMarkerClicked = { place ->
        fragment.detailsSheetHelper.passInfoToSheet(
            context = fragment.requireContext(),
            place = place,
            mediaList = fragment.mediaList,
            zoomLevel = binding.mapView.zoomLevelDouble,
            onMediaClicked = { index ->
                (fragment.parentFragment as? fr.free.nrw.commons.explore.ExploreMapRootFragment)
                    ?.onMediaClicked(index)
            },
        )
    }
    mediaListProvider = { fragment.mediaList }
}

/**
 * Creates and configures a ScaleDiskOverlay centered at the given GeoPoint.
 *
 * @param context Android context
 * @param mapCenter Coordinates of the circle center
 * @return Configured ScaleDiskOverlay instance
 */
fun createScaleDiskOverlay(
    context: Context,
    mapCenter: GeoPoint,
): ScaleDiskOverlay =
    ScaleDiskOverlay(
        context,
        mapCenter,
        SCALE_DISK_RADIUS_FEET,
        GeoConstants.UnitOfMeasure.foot,
    ).apply {
        setCirclePaint2(
            Paint().apply {
                color = Color.rgb(SCALE_DISK_GRAY, SCALE_DISK_GRAY, SCALE_DISK_GRAY)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            },
        )
        setCirclePaint1(
            Paint().apply {
                color = Color.argb(SCALE_DISK_ALPHA, SCALE_DISK_GRAY, SCALE_DISK_GRAY, SCALE_DISK_GRAY)
                style = Paint.Style.FILL_AND_STROKE
            },
        )
        setDisplaySizeMin(SCALE_DISK_MIN_SIZE)
        setDisplaySizeMax(SCALE_DISK_MAX_SIZE)
    }
