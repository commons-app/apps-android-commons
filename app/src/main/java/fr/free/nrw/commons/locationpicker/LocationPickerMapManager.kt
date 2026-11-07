package fr.free.nrw.commons.locationpicker

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.core.content.ContextCompat
import fr.free.nrw.commons.CameraPosition
import fr.free.nrw.commons.R
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.utils.handleGeoCoordinates
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.osmdroid.views.overlay.TilesOverlay

/**
 * Manages map movement and location markers for [LocationPickerActivity].
 */
class LocationPickerMapManager(
    private val context: Context,
    private val mapView: MapView,
    private val markerImage: ImageView,
    private val locationManager: LocationServiceManager,
) {
    /**
     * Applies the dark map colors when the device uses a dark theme.
     */
    fun applyDarkTheme(
        isDarkTheme: Boolean,
        shadow: ImageView,
    ) {
        if (isDarkTheme) {
            shadow.setColorFilter(Color.argb(FULL_ALPHA, FULL_COLOR, FULL_COLOR, FULL_COLOR))
            mapView.overlayManager.tilesOverlay.setColorFilter(TilesOverlay.INVERT_COLORS)
        }
    }

    /**
     * Moves the center of the map to the specified coordinates.
     */
    fun moveMapTo(
        latitude: Double,
        longitude: Double,
    ) {
        val point = GeoPoint(latitude, longitude)
        mapView.controller.setCenter(point)
        mapView.controller.animateTo(point)
    }

    /**
     * Moves the center of the map to the specified point when it is available.
     */
    fun moveMapTo(point: GeoPoint?) {
        point?.let {
            moveMapTo(it.latitude, it.longitude)
        }
    }

    /**
     * Moves the map to the coordinates stored for the selected media.
     */
    fun moveMapToMediaLocation(cameraPosition: CameraPosition?) {
        cameraPosition?.let {
            moveMapTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    /**
     * Moves the map to the device's last known location.
     */
    fun moveMapToGPSLocation() {
        locationManager.getLastLocation()?.let {
            moveMapTo(GeoPoint(it.latitude, it.longitude))
        }
    }

    /**
     * Opens the selected location in an installed map application.
     */
    fun showInMapApp(
        pickerSource: String?,
        cameraPosition: CameraPosition?,
    ) {
        val position =
            if (pickerSource == UPLOAD_ACTIVITY && cameraPosition != null) {
                LatLng(cameraPosition.latitude, cameraPosition.longitude, 0.0f)
            } else {
                LatLng(mapView.mapCenter.latitude, mapView.mapCenter.longitude, 0.0f)
            }

        handleGeoCoordinates(context, position, mapView.zoomLevelDouble)
    }

    /**
     * Shows a marker at the selected location.
     */
    fun showSelectedLocationMarker(point: GeoPoint) {
        val icon = ContextCompat.getDrawable(context, R.drawable.map_default_map_marker)
        Marker(mapView).apply {
            position = point
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            setIcon(icon)
            infoWindow = null
            mapView.overlays.add(this)
        }
        mapView.invalidate()
    }

    /**
     * Removes the marker for the selected media location.
     */
    fun removeSelectedLocationMarker(cameraPosition: CameraPosition?) {
        mapView.overlays
            .filterIsInstance<Marker>()
            .firstOrNull {
                it.position.latitude == cameraPosition?.latitude &&
                    it.position.longitude == cameraPosition.longitude
            }?.let {
                mapView.overlays.remove(it)
                mapView.invalidate()
            }
    }

    /**
     * Adds a marker at the device's last known location.
     */
    fun addMarkerAtGPSLocation(moveToCurrentLocation: Boolean) {
        locationManager.getLastLocation()?.let {
            addLocationMarker(GeoPoint(it.latitude, it.longitude), moveToCurrentLocation)
            markerImage.translationY = 0f
        }
    }

    /**
     * Adds the current-location marker and its accuracy overlay to the map.
     */
    fun addLocationMarker(
        geoPoint: GeoPoint,
        moveToCurrentLocation: Boolean,
    ) {
        if (moveToCurrentLocation) {
            mapView.overlays.clear()
        }

        val diskOverlay =
            ScaleDiskOverlay(
                context,
                geoPoint,
                LOCATION_ACCURACY_RADIUS,
                GeoConstants.UnitOfMeasure.foot,
            )

        val circlePaint =
            Paint().apply {
                color = Color.rgb(MARKER_COLOR, MARKER_COLOR, MARKER_COLOR)
                style = Paint.Style.STROKE
                strokeWidth = 2f
            }
        diskOverlay.setCirclePaint2(circlePaint)

        val diskPaint =
            Paint().apply {
                color = Color.argb(MARKER_ALPHA, MARKER_COLOR, MARKER_COLOR, MARKER_COLOR)
                style = Paint.Style.FILL_AND_STROKE
            }
        diskOverlay.setCirclePaint1(diskPaint)
        diskOverlay.setDisplaySizeMin(MINIMUM_DISPLAY_SIZE)
        diskOverlay.setDisplaySizeMax(MAXIMUM_DISPLAY_SIZE)
        mapView.overlays.add(diskOverlay)

        val startMarker =
            Marker(mapView).apply {
                position = geoPoint
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                icon = ContextCompat.getDrawable(context, R.drawable.current_location_marker)
                title = "Your Location"
                textLabelFontSize = MARKER_LABEL_SIZE
            }

        mapView.overlays.add(startMarker)
    }

    private companion object {
        const val FULL_ALPHA = 255
        const val FULL_COLOR = 255
        const val LOCATION_ACCURACY_RADIUS = 2000
        const val MARKER_COLOR = 128
        const val MARKER_ALPHA = 40
        const val MINIMUM_DISPLAY_SIZE = 900
        const val MAXIMUM_DISPLAY_SIZE = 1700
        const val MARKER_LABEL_SIZE = 24
        const val UPLOAD_ACTIVITY = "UploadActivity"
    }
}
