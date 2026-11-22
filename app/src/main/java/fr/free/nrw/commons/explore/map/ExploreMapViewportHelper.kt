package fr.free.nrw.commons.explore.map

import android.app.Activity
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.text.method.LinkMovementMethod
import android.view.View
import androidx.annotation.VisibleForTesting
import androidx.preference.PreferenceManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import fr.free.nrw.commons.R
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationPermissionsHelper
import fr.free.nrw.commons.utils.MapUtils.ZOOM_LEVEL
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.TilesOverlay

private const val DEFAULT_MAP_ACCURACY = 1f
private const val DEFAULT_MAP_FOCUS_ACCURACY = 100f
private const val SEARCH_THIS_AREA_THRESHOLD_METERS = 2000.0
private const val LOCATION_LAT_INDEX = 0
private const val LOCATION_LNG_INDEX = 1

/**
 * Helper class that manages the map camera viewport, focus, recenter action,
 * and search-this-area button visibility for ExploreMapFragment.
 */
class ExploreMapViewportHelper : ExploreMapContract.Viewport {
    @VisibleForTesting
    var mapView: MapView? = null

    @VisibleForTesting
    var fabRecenter: FloatingActionButton? = null

    @VisibleForTesting
    var searchThisAreaButton: View? = null

    @VisibleForTesting
    var applicationKvStore: JsonKvStore? = null

    @VisibleForTesting
    var locationPermissionsHelper: LocationPermissionsHelper? = null

    var activityProvider: (() -> Activity?)? = null
    var onLocationPermissionRequest: (() -> Unit)? = null
    var onPermissionGranted: (() -> Unit)? = null
    var onMarkerRecenter: ((GeoPoint) -> Unit)? = null
    var onNetworkCheck: (() -> Boolean)? = null
    var onSearchThisAreaClicked: (() -> View.OnClickListener?)? = null

    var lastMapFocus: GeoPoint? = null
    var mapCenter: GeoPoint? = null
    var lastKnownLocation: LatLng? = null
    var isPermissionDenied: Boolean = false
    var recenterToUserLocation: Boolean = false
    var prevZoom: Double = 0.0
    var prevLatitude: Double = 0.0
    var prevLongitude: Double = 0.0

    /**
     * Checks if fragment arguments contain data from Nearby map, indicating that the user navigated
     * from Nearby using 'Show in Explore'.
     *
     * @return true if user navigated from Nearby map
     */
    val isCameFromNearbyMap: Boolean
        get() = prevZoom != 0.0 || prevLatitude != 0.0 || prevLongitude != 0.0

    /**
     * Returns the LatLng from the previous Fragment's map center or (0,0,0) coordinates
     * if that information is not available/applicable.
     */
    val previousLatLng: LatLng
        get() = LatLng(prevLatitude, prevLongitude, prevZoom.toFloat())

    /**
     * Returns the current map center coordinates.
     *
     * @return LatLng of map center or default fallback location
     */
    override fun getMapCenter(): LatLng? =
        if (mapCenter != null) {
            LatLng(mapCenter!!.latitude, mapCenter!!.longitude, DEFAULT_MAP_FOCUS_ACCURACY)
        } else {
            val lastLocationStr = applicationKvStore?.getString("LastLocation")
            if (lastLocationStr != null) {
                val locationLatLng =
                    lastLocationStr
                        .split(",".toRegex())
                        .dropLastWhile { it.isEmpty() }
                        .toTypedArray()
                lastKnownLocation =
                    LatLng(
                        locationLatLng[LOCATION_LAT_INDEX]!!.toDouble(),
                        locationLatLng[LOCATION_LNG_INDEX]!!.toDouble(),
                        DEFAULT_MAP_ACCURACY,
                    )
                lastKnownLocation
            } else {
                defaultLatLng
            }
        }

    /**
     * Returns the focus coordinate of the map view.
     *
     * @return LatLng of the current map center focus
     */
    override fun getMapFocus(): LatLng? {
        val map = mapView ?: return null
        return LatLng(
            map.mapCenter.latitude,
            map.mapCenter.longitude,
            DEFAULT_MAP_FOCUS_ACCURACY,
        )
    }

    /**
     * Returns the last recorded map focus coordinates.
     *
     * @return LatLng representing last focus or current center if not yet recorded
     */
    override fun getLastMapFocus(): LatLng? =
        if (lastMapFocus == null) {
            getMapCenter()
        } else {
            LatLng(lastMapFocus!!.latitude, lastMapFocus!!.longitude, DEFAULT_MAP_FOCUS_ACCURACY)
        }

    /**
     * Recenters the map and updates location marker and search-this-area button state.
     *
     * @param curLatLng Target coordinates to center the map onto
     */
    override fun recenterMap(curLatLng: LatLng?) {
        // if user has denied permission twice, then show dialog
        val act = activityProvider?.invoke()
        if (isPermissionDenied) {
            if (act != null && locationPermissionsHelper?.checkLocationPermission(act) == true) {
                // this will run when user has given permission by opening app's settings
                isPermissionDenied = false
                recenterMap(curLatLng)
            } else {
                onLocationPermissionRequest?.invoke()
            }
        } else {
            if (act == null || locationPermissionsHelper?.checkLocationPermission(act) != true) {
                onLocationPermissionRequest?.invoke()
            } else {
                onPermissionGranted?.invoke()
            }
        }
        if (curLatLng == null) {
            recenterToUserLocation = true
            return
        }
        val targetPoint = GeoPoint(curLatLng.latitude, curLatLng.longitude)
        onMarkerRecenter?.invoke(targetPoint)
        mapView?.controller?.animateTo(targetPoint)

        val focus = lastMapFocus
        val map = mapView
        if (focus != null && map != null) {
            val mylocation = Location("")
            val destLocation = Location("")
            destLocation.latitude = map.mapCenter.latitude
            destLocation.longitude = map.mapCenter.longitude
            mylocation.latitude = focus.latitude
            mylocation.longitude = focus.longitude
            val distance = mylocation.distanceTo(destLocation) // in meters
            if (onNetworkCheck?.invoke() == true) {
                setSearchThisAreaButtonVisibility(distance > SEARCH_THIS_AREA_THRESHOLD_METERS)
            }
        } else {
            setSearchThisAreaButtonVisibility(false)
        }
    }

    /**
     * Disables recentering floating action button.
     */
    override fun disableFABRecenter() {
        fabRecenter?.isEnabled = false
    }

    /**
     * Enables recentering floating action button.
     */
    override fun enableFABRecenter() {
        fabRecenter?.isEnabled = true
    }

    /**
     * Sets click listener on the recenter floating action button.
     *
     * @param onClickListener The OnClickListener to attach
     */
    override fun setFABRecenterAction(onClickListener: View.OnClickListener?) {
        fabRecenter?.setOnClickListener(onClickListener)
    }

    /**
     * Attaches click action to search-this-area button.
     */
    override fun addSearchThisAreaButtonAction() {
        searchThisAreaButton?.setOnClickListener(onSearchThisAreaClicked?.invoke())
    }

    /**
     * Sets visibility of search-this-area button.
     *
     * @param isVisible True to show button, false to hide
     */
    override fun setSearchThisAreaButtonVisibility(isVisible: Boolean) {
        searchThisAreaButton?.visibility = if (isVisible) View.VISIBLE else View.GONE
    }

    /**
     * Returns whether search-this-area button is visible.
     *
     * @return True if button is currently visible
     */
    override fun isSearchThisAreaButtonVisible(): Boolean = searchThisAreaButton?.visibility == View.VISIBLE
}

/**
 * Fetch Nearby map camera data from fragment arguments if any.
 *
 * @param arguments Fragment arguments bundle
 */
fun ExploreMapViewportHelper.loadNearbyMapData(arguments: Bundle?) {
    if (arguments != null) {
        if (arguments.containsKey("prev_zoom")) prevZoom = arguments.getDouble("prev_zoom")
        if (arguments.containsKey("prev_latitude")) prevLatitude = arguments.getDouble("prev_latitude")
        if (arguments.containsKey("prev_longitude")) prevLongitude = arguments.getDouble("prev_longitude")
    }
}

/**
 * Binds viewport UI controls and fragment callbacks to the helper.
 *
 * @param binding Layout binding containing map and action buttons
 * @param fragment Host ExploreMapFragment instance
 */
fun ExploreMapViewportHelper.attachViewportHelper(
    binding: FragmentExploreMapBinding,
    fragment: ExploreMapFragment,
) {
    mapView = binding.mapView
    fabRecenter = binding.fabRecenter
    searchThisAreaButton = binding.searchThisAreaButton
    applicationKvStore = fragment.applicationKvStore
    locationPermissionsHelper = fragment.locationPermissionsHelper
    activityProvider = { fragment.activity }
    onLocationPermissionRequest = { fragment.askForLocationPermission() }
    onPermissionGranted = { fragment.onLocationPermissionGrantedAction() }
    onMarkerRecenter = { fragment.markerOverlayHelper.recenterMarkerToPosition(it) }
    onNetworkCheck = { fragment.isNetworkConnectionEstablished() }
    onSearchThisAreaClicked = { fragment.presenter?.onSearchThisAreaClicked() }
}

/**
 * Configures the MapView with tile sources, overlays, and gesture listeners.
 *
 * @param fragment The hosting ExploreMapFragment
 */
fun FragmentExploreMapBinding.configureExploreMap(fragment: ExploreMapFragment) {
    tvAttribution.text =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(root.context.getString(R.string.map_attribution), Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(root.context.getString(R.string.map_attribution))
        }
    tvAttribution.movementMethod = LinkMovementMethod.getInstance()

    Configuration.getInstance().load(
        root.context,
        PreferenceManager.getDefaultSharedPreferences(root.context),
    )
    Configuration.getInstance().additionalHttpRequestProperties["Referer"] =
        "http://maps.wikimedia.org/"

    mapView.setTileSource(TileSourceFactory.WIKIMEDIA)
    mapView.setTilesScaledToDpi(true)
    mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
    mapView.setMultiTouchControls(true)

    if (fragment.systemThemeUtils.isDeviceInNightMode()) {
        mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
    }

    if (!fragment.viewportHelper.isCameFromNearbyMap) {
        mapView.controller.setZoom(ZOOM_LEVEL.toDouble())
    }

    mapView.overlays.add(
        MapEventsOverlay(
            object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint?): Boolean {
                    fragment.markerOverlayHelper.handleMapTap(fragment.mediaList)
                    return true
                }

                override fun longPressHelper(p: GeoPoint?): Boolean = false
            },
        ),
    )
    mapView.addMapListener(
        object : MapListener {
            override fun onScroll(event: ScrollEvent): Boolean {
                val focus = fragment.viewportHelper.lastMapFocus
                val hasUserScrolled = event.x > 0 || event.y > 0
                if (focus != null && hasUserScrolled && fragment.isNetworkConnectionEstablished()) {
                    val myLoc =
                        Location("").apply {
                            latitude = focus.latitude
                            longitude = focus.longitude
                        }
                    val dest =
                        Location("").apply {
                            latitude = mapView.mapCenter.latitude
                            longitude =
                                mapView.mapCenter.longitude
                        }
                    fragment.setSearchThisAreaButtonVisibility(
                        myLoc.distanceTo(dest) > SEARCH_THIS_AREA_THRESHOLD_METERS,
                    )
                } else {
                    fragment.setSearchThisAreaButtonVisibility(false)
                }
                return true
            }

            override fun onZoom(event: ZoomEvent?): Boolean = false
        },
    )
}
