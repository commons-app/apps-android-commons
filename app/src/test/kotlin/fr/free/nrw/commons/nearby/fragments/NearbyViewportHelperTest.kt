package fr.free.nrw.commons.nearby.fragments

import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.api.IMapController
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.Projection
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for [NearbyViewportHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyViewportHelperTest {
    private lateinit var helper: NearbyViewportHelper
    private lateinit var activity: AppCompatActivity
    private lateinit var mapView: MapView
    private lateinit var mapController: IMapController
    private lateinit var projection: Projection

    private var highlightedPlace: Place? = null
    private var requestedRecenterLatLng: LatLng? = null

    private val actions =
        object : NearbyViewportHelper.NearbyViewportActions {
            override fun onPlaceHighlighted(place: Place) {
                highlightedPlace = place
            }

            override fun onRecenterMapRequested(currentLatLng: LatLng?) {
                requestedRecenterLatLng = currentLatLng
            }
        }

    /**
     * Prepares test fixtures, mocks, and initializes the helper.
     */
    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(AppCompatActivity::class.java).create().get()
        mapView = mock()
        mapController = mock()
        projection = mock()

        whenever(mapView.controller).thenReturn(mapController)
        whenever(mapView.projection).thenReturn(projection)

        helper = NearbyViewportHelper()
        helper.init(
            mapViewProvider = { mapView },
            activityProvider = { activity },
            actions = actions,
        )
    }

    @Test
    fun testGetCameraTargetWithNullMapReturnsNull() {
        val nullHelper = NearbyViewportHelper()
        nullHelper.init(
            mapViewProvider = { null },
            activityProvider = { activity },
            actions = actions,
        )
        assertNull(nullHelper.getCameraTarget())
    }

    @Test
    fun testGetCameraTargetWithMapReturnsFocus() {
        whenever(mapView.mapCenter).thenReturn(GeoPoint(12.34, 56.78))
        val target = helper.getCameraTarget()
        assertNotNull(target)
        assertEquals(12.34, target!!.latitude, 0.001)
        assertEquals(56.78, target.longitude, 0.001)
    }

    @Test
    fun testCenterMapToPlacePortrait() {
        activity.resources.configuration.orientation = Configuration.ORIENTATION_PORTRAIT
        val place =
            Place(
                "Q1",
                "Test Place",
                Label.FOREST,
                "Description",
                LatLng(48.8584, 2.2945, 1f),
                "Category",
                null,
                "Pic",
                false,
            )

        helper.centerMapToPlace(place)

        assertEquals(place, highlightedPlace)
        assertNotNull(requestedRecenterLatLng)
        // In portrait, latitude is shifted by 0.005
        assertEquals(48.8584 - 0.005, requestedRecenterLatLng!!.latitude, 0.0001)
        assertEquals(2.2945, requestedRecenterLatLng!!.longitude, 0.0001)
    }

    @Test
    fun testCenterMapToPlaceLandscape() {
        activity.resources.configuration.orientation = Configuration.ORIENTATION_LANDSCAPE
        val place =
            Place(
                "Q2",
                "Landscape Place",
                Label.PARK,
                "Description",
                LatLng(48.8584, 2.2945, 1f),
                "Category",
                null,
                "Pic",
                false,
            )

        helper.centerMapToPlace(place)

        assertEquals(place, highlightedPlace)
        assertNotNull(requestedRecenterLatLng)
        // In landscape, latitude is shifted by 0.002
        assertEquals(48.8584 - 0.002, requestedRecenterLatLng!!.latitude, 0.0001)
        assertEquals(2.2945, requestedRecenterLatLng!!.longitude, 0.0001)
    }

    @Test
    fun testGetLastMapFocusStored() {
        helper.lastMapFocus = GeoPoint(10.0, 20.0)
        val latLng = helper.getLastMapFocus()
        assertEquals(10.0, latLng.latitude, 0.001)
        assertEquals(20.0, latLng.longitude, 0.001)
    }

    @Test
    fun testGetLastMapFocusDefault() {
        helper.lastMapFocus = null
        val latLng = helper.getLastMapFocus()
        assertNotNull(latLng)
    }

    @Test
    fun testGetMapFocus() {
        whenever(mapView.mapCenter).thenReturn(GeoPoint(35.0, 45.0))
        val latLng = helper.getMapFocus()
        assertEquals(35.0, latLng.latitude, 0.001)
        assertEquals(45.0, latLng.longitude, 0.001)
    }

    @Test
    fun testGetScreenTopRightAndBottomLeftWithNullMap() {
        val nullHelper = NearbyViewportHelper()
        nullHelper.init(
            mapViewProvider = { null },
            activityProvider = { activity },
            actions = actions,
        )
        assertNull(nullHelper.getScreenTopRight())
        assertNull(nullHelper.getScreenBottomLeft())
    }

    @Test
    fun testGetScreenTopRightWithMap() {
        whenever(mapView.width).thenReturn(800)
        whenever(mapView.height).thenReturn(1200)
        whenever(projection.fromPixels(800, 0)).thenReturn(GeoPoint(50.0, 15.0))

        val topRight = helper.getScreenTopRight()
        assertNotNull(topRight)
        assertEquals(50.0, topRight!!.latitude, 0.001)
        assertEquals(15.0, topRight.longitude, 0.001)
    }

    @Test
    fun testGetScreenBottomLeftWithMap() {
        whenever(mapView.width).thenReturn(800)
        whenever(mapView.height).thenReturn(1200)
        whenever(projection.fromPixels(0, 1200)).thenReturn(GeoPoint(45.0, 10.0))

        val bottomLeft = helper.getScreenBottomLeft()
        assertNotNull(bottomLeft)
        assertEquals(45.0, bottomLeft!!.latitude, 0.001)
        assertEquals(10.0, bottomLeft.longitude, 0.001)
    }

    @Test
    fun testMoveCameraToPosition() {
        val target = GeoPoint(10.0, 20.0)
        helper.moveCameraToPosition(target)
        verify(mapController).animateTo(target)
    }

    @Test
    fun testMoveCameraToPositionWithZoomAndSpeed() {
        val target = GeoPoint(10.0, 20.0)
        helper.moveCameraToPosition(target, 16.0, 1000L)
        verify(mapController).animateTo(target, 16.0, 1000L)
    }

    @Test
    fun testCalculateBoundingBoxZoomNormalDimensions() {
        whenever(mapView.width).thenReturn(1000)
        whenever(mapView.height).thenReturn(1000)
        val boundingBox = BoundingBox(52.0, 14.0, 51.0, 13.0)

        val zoom = mapView.calculateBoundingBoxZoom(boundingBox, 50)
        assertTrue(zoom > 0.0)
    }

    @Test
    fun testCalculateBoundingBoxZoomZeroDimensionsReturnsDefault() {
        whenever(mapView.width).thenReturn(0)
        whenever(mapView.height).thenReturn(0)
        val boundingBox = BoundingBox(52.0, 14.0, 51.0, 13.0)

        val zoom = mapView.calculateBoundingBoxZoom(boundingBox, 50)
        assertEquals(15.0, zoom, 0.001)
    }
}
