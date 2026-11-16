package fr.free.nrw.commons.nearby.fragments

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.util.constants.GeoConstants.UnitOfMeasure
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.ScaleBarOverlay
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.concurrent.AbstractExecutorService
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [NearbyMarkerOverlayHelper].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class NearbyMarkerOverlayHelperTest {
    private lateinit var helper: NearbyMarkerOverlayHelper
    private lateinit var context: Context
    private lateinit var mapView: MapView
    private lateinit var locationManager: LocationServiceManager
    private lateinit var markerCallback: NearbyMarkerOverlayHelper.MarkerCallback
    private lateinit var mapState: NearbyMarkerOverlayHelper.MapStateProvider

    private var nearestPlace: Place? = null
    private var mapEventsOverlay: MapEventsOverlay? = null
    private var isPermissionDenied: Boolean = false
    private var isDepictsMode: Boolean = false
    private var allPhotoCoords: List<LatLng>? = null

    private val directExecutor =
        object : AbstractExecutorService() {
            override fun shutdown() = Unit

            override fun shutdownNow(): List<Runnable> = emptyList()

            override fun isShutdown(): Boolean = false

            override fun isTerminated(): Boolean = false

            override fun awaitTermination(
                timeout: Long,
                unit: TimeUnit,
            ): Boolean = true

            override fun execute(command: Runnable) = command.run()
        }

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.LightAppTheme)
        mapView = MapView(context)
        locationManager = mock()
        markerCallback = mock()

        nearestPlace = null
        mapEventsOverlay = mock()
        isPermissionDenied = false
        isDepictsMode = false
        allPhotoCoords = null

        mapState =
            object : NearbyMarkerOverlayHelper.MapStateProvider {
                override fun getMapCenter(): GeoPoint? = GeoPoint(10.0, 20.0)

                override fun getMapEventsOverlay(): MapEventsOverlay? = mapEventsOverlay

                override fun isPermissionDenied(): Boolean = isPermissionDenied

                override fun isDepictsPickerMode(): Boolean = isDepictsMode

                override fun getAllPhotoLatLngs(): List<LatLng>? = allPhotoCoords

                override fun getNearestPlace(): Place? = nearestPlace
            }

        helper = NearbyMarkerOverlayHelper(executor = directExecutor)
        helper.init(
            mapViewProvider = { mapView },
            contextProvider = { context },
            locationManagerProvider = { locationManager },
            mapStateProvider = mapState,
            markerCallback = markerCallback,
        )
    }

    @Test
    fun testMarkerPriorityOrder() {
        val nearest =
            Place().apply {
                name = "Nearest Place"
                isMonument = false
                pic = ""
                exists = true
            }
        val needsPhoto =
            Place().apply {
                name = "Needs Photo"
                isMonument = false
                pic = ""
                exists = true
            }
        val monument =
            Place().apply {
                name = "Monument"
                isMonument = true
                pic = ""
                exists = true
            }
        val hasPhoto =
            Place().apply {
                name = "Has Photo"
                isMonument = false
                pic = "pic.jpg"
                exists = true
            }
        val grey =
            Place().apply {
                name = ""
                isMonument = false
                pic = ""
                exists = true
            }
        val destroyed =
            Place().apply {
                name = "Destroyed"
                isMonument = false
                pic = ""
                exists = false
            }

        val pNearest = getMarkerPriority(nearest, false, nearest)
        val pNeedsPhoto = getMarkerPriority(needsPhoto, false, null)
        val pMonument = getMarkerPriority(monument, false, null)
        val pHasPhoto = getMarkerPriority(hasPhoto, false, null)
        val pGrey = getMarkerPriority(grey, false, null)
        val pDestroyed = getMarkerPriority(destroyed, false, null)

        assertTrue(pNearest > pNeedsPhoto)
        assertTrue(pNeedsPhoto > pMonument)
        assertTrue(pMonument > pHasPhoto)
        assertTrue(pHasPhoto > pGrey)
        assertTrue(pGrey > pDestroyed)
    }

    @Test
    fun testGetIconForPhotoStates() {
        val target = Place().apply { name = "Target" }
        assertEquals(
            R.drawable.ic_custom_map_marker_purple,
            getIconFor(target, false, target),
        )
        assertEquals(
            R.drawable.ic_custom_map_marker_purple_bookmarked,
            getIconFor(target, true, target),
        )

        val hasPic =
            Place().apply {
                name = "P"
                pic = "photo.jpg"
                exists = true
            }
        assertEquals(
            R.drawable.ic_custom_map_marker_green,
            getIconFor(hasPic, false, null),
        )
        assertEquals(
            R.drawable.ic_custom_map_marker_green_bookmarked,
            getIconFor(hasPic, true, null),
        )

        val normal =
            Place().apply {
                name = "N"
                pic = ""
                exists = true
            }
        assertEquals(
            R.drawable.ic_custom_map_marker_red,
            getIconFor(normal, false, null),
        )
        assertEquals(
            R.drawable.ic_custom_map_marker_red_bookmarked,
            getIconFor(normal, true, null),
        )
    }

    @Test
    fun testGetIconForSpecialTypes() {
        val monument =
            Place().apply {
                name = "M"
                isMonument = true
            }
        assertEquals(
            R.drawable.ic_custom_map_marker_monuments,
            getIconFor(monument, false, null),
        )

        val destroyed =
            Place().apply {
                name = "D"
                exists = false
            }
        assertEquals(
            R.drawable.ic_clear_black_24dp,
            getIconFor(destroyed, false, null),
        )

        val noName =
            Place().apply {
                name = ""
                exists = true
            }
        assertEquals(
            R.drawable.ic_custom_map_marker_grey,
            getIconFor(noName, false, null),
        )
        assertEquals(
            R.drawable.ic_custom_map_marker_grey_bookmarked,
            getIconFor(noName, true, null),
        )
    }

    @Test
    fun testClearAllMarkersAddsBaseOverlays() {
        whenever(locationManager.getLastLocation()).thenReturn(LatLng(10.0, 20.0, 15f))

        helper.clearAllMarkers()

        assertTrue(mapView.overlays.any { it is ScaleBarOverlay })
        assertTrue(mapView.overlays.any { it is ScaleDiskOverlay })
        assertTrue(mapView.overlays.any { it is Marker && it.title == "Your Location" })
        assertTrue(mapView.overlays.contains(mapEventsOverlay))
    }

    @Test
    fun testFilterOutAllMarkersClearsOverlays() {
        helper.filterOutAllMarkers()
        assertTrue(mapView.overlays.any { it is ScaleBarOverlay })
    }

    @Test
    fun testReplaceMarkerOverlaysSortsByPriority() {
        val placeNeedsPhoto =
            Place().apply {
                name = "Needs Photo"
                location = LatLng(10.0, 20.0, 0f)
                pic = ""
                exists = true
            }
        val placeHasPhoto =
            Place().apply {
                name = "Has Photo"
                location = LatLng(10.1, 20.1, 0f)
                pic = "photo.jpg"
                exists = true
            }

        val groups =
            listOf(
                MarkerPlaceGroup(false, placeNeedsPhoto),
                MarkerPlaceGroup(false, placeHasPhoto),
            )

        helper.replaceMarkerOverlays(groups)

        val markers = mapView.overlays.filterIsInstance<Marker>().filter { it.title != "Your Location" }
        assertEquals(2, markers.size)
        // Red pin (higher priority) should be drawn after Green pin
        assertEquals("Has Photo", markers[0].title)
        assertEquals("Needs Photo", markers[1].title)
    }

    @Test
    fun testFilterMarkersByLabelsEmptyRestoresDefault() {
        val place =
            Place().apply {
                name = "Default Place"
                location = LatLng(10.0, 20.0, 0f)
                exists = true
                pic = ""
            }
        NearbyController.markerLabelList.clear()
        NearbyController.markerLabelList.add(MarkerPlaceGroup(false, place))

        helper.filterMarkersByLabels(emptyList(), filterForPlaceState = false, filterForAllNoneType = false)

        val markers = mapView.overlays.filterIsInstance<Marker>().filter { it.title != "Your Location" }
        assertEquals(1, markers.size)
        assertEquals("Default Place", markers[0].title)
    }

    @Test
    fun testFilterMarkersByLabelsWithSelection() {
        val place1 =
            Place().apply {
                name = "Place 1"
                location = LatLng(10.0, 20.0, 0f)
                label = Label.BUILDING
                exists = true
                pic = ""
            }
        val place2 =
            Place().apply {
                name = "Place 2"
                location = LatLng(10.1, 20.1, 0f)
                label = Label.PARK
                exists = true
                pic = ""
            }
        NearbyController.markerLabelList.clear()
        NearbyController.markerLabelList.add(MarkerPlaceGroup(false, place1))
        NearbyController.markerLabelList.add(MarkerPlaceGroup(false, place2))

        helper.filterMarkersByLabels(
            listOf(Label.BUILDING),
            filterForPlaceState = false,
            filterForAllNoneType = false,
        )

        val markers = mapView.overlays.filterIsInstance<Marker>().filter { it.title != "Your Location" }
        assertEquals(1, markers.size)
        assertEquals("Place 1", markers[0].title)
    }

    @Test
    fun testAddCurrentLocationMarkerWithGpsDisabledDoesNothing() {
        whenever(locationManager.isGPSProviderEnabled()).thenReturn(false)
        helper.addCurrentLocationMarker(LatLng(10.0, 20.0, 0f))
        assertNull(helper.userLocationOverlay)
    }

    @Test
    fun testAddCurrentLocationMarkerWithNullLocationDoesNothing() {
        helper.addCurrentLocationMarker(null)
        assertNull(helper.userLocationOverlay)
    }

    @Test
    fun testRecenterMarkerToPositionUpdatesOverlays() {
        whenever(locationManager.getLastLocation()).thenReturn(LatLng(12.0, 34.0, 10f))
        helper.recenterMarkerToPosition(GeoPoint(12.0, 34.0))

        assertNotNull(helper.userLocationOverlay)
        assertNotNull(helper.userLocationErrorOverlay)
        assertTrue(mapView.overlays.contains(helper.userLocationOverlay))
        assertTrue(mapView.overlays.contains(helper.userLocationErrorOverlay))
    }

    @Test
    fun testAddPhotoMarkersInDepictsMode() {
        isDepictsMode = true
        allPhotoCoords =
            listOf(
                LatLng(10.0, 20.0, 0f),
                LatLng(11.0, 21.0, 0f),
            )

        helper.addPhotoMarkerIfDepictsMode()

        assertNotNull(helper.photoPinBoundingBox)
        val photoMarkers = mapView.overlays.filterIsInstance<Marker>().filter { it.title?.startsWith("Photo") == true }
        assertEquals(2, photoMarkers.size)
    }

    @Test
    fun testConvertToMarkerClickTriggersCallback() {
        var clickedPlace: Place? = null
        val testCallback =
            object : NearbyMarkerOverlayHelper.MarkerCallback {
                override fun onMarkerClicked(
                    place: Place,
                    marker: Marker,
                    isBookmarked: Boolean,
                ) {
                    clickedPlace = place
                }

                override fun onPhotoMarkerClicked(marker: Marker) = Unit

                override fun onBoundingBoxZoomCalculated(boundingBox: BoundingBox) = Unit
            }
        helper.init(
            mapViewProvider = { mapView },
            contextProvider = { context },
            locationManagerProvider = { locationManager },
            mapStateProvider = mapState,
            markerCallback = testCallback,
        )

        val place =
            Place().apply {
                name = "Test Click"
                location = LatLng(10.0, 20.0, 0f)
                exists = true
                pic = ""
            }

        val markerContext =
            MarkerContext(
                context = context,
                mapView = mapView,
                nearestPlace = null,
                cache = helper.drawableCache,
                onMarkerClicked = { p, m, b ->
                    testCallback.onMarkerClicked(p, m, b)
                },
            )

        val marker = convertToMarker(place, false, markerContext)
        val listenerField =
            Marker::class.java.getDeclaredField("mOnMarkerClickListener").apply {
                isAccessible = true
            }
        val listener = listenerField.get(marker) as Marker.OnMarkerClickListener
        listener.onMarkerClick(marker, mapView)

        assertEquals("Test Click", clickedPlace?.name)
    }

    @Test
    fun testCreateCurrentLocationErrorOverlay() {
        val overlay =
            createCurrentLocationErrorOverlay(
                context = context,
                point = GeoPoint(10.0, 20.0),
                value = 50,
                unitOfMeasure = UnitOfMeasure.meter,
            )
        assertNotNull(overlay)
    }

    @Test
    fun testGetCachedDrawable() {
        val d1 = getCachedDrawable(context, R.drawable.current_location_marker, helper.drawableCache)
        assertNotNull(d1)
        val d2 = getCachedDrawable(context, R.drawable.current_location_marker, helper.drawableCache)
        assertEquals(d1, d2)
        assertNull(getCachedDrawable(null, null, null))
    }

    @Test
    fun testAddCurrentLocationMarkerWithGpsEnabledAddsMarker() {
        whenever(locationManager.isGPSProviderEnabled()).thenReturn(true)
        whenever(locationManager.getLastLocation()).thenReturn(LatLng(10.0, 20.0, 5f))

        helper.addCurrentLocationMarker(LatLng(10.0, 20.0, 0f))

        assertNotNull(helper.userLocationOverlay)
        assertNotNull(helper.userLocationErrorOverlay)
        assertTrue(mapView.overlays.contains(helper.userLocationOverlay))
        assertTrue(mapView.overlays.contains(helper.userLocationErrorOverlay))
    }

    @Test
    fun testFilterMarkersByLabelsWithBookmarks() {
        val bookmarkedPlace =
            Place().apply {
                name = "Bookmarked Place"
                location = LatLng(10.0, 20.0, 0f)
                exists = true
                pic = ""
            }
        val normalPlace =
            Place().apply {
                name = "Normal Place"
                location = LatLng(10.1, 20.1, 0f)
                exists = true
                pic = ""
            }
        NearbyController.markerLabelList.clear()
        NearbyController.markerLabelList.add(MarkerPlaceGroup(true, bookmarkedPlace))
        NearbyController.markerLabelList.add(MarkerPlaceGroup(false, normalPlace))

        helper.filterMarkersByLabels(
            listOf(Label.BOOKMARKS),
            filterForPlaceState = false,
            filterForAllNoneType = false,
        )

        val markers = mapView.overlays.filterIsInstance<Marker>().filter { it.title != "Your Location" }
        assertEquals(1, markers.size)
        assertEquals("Bookmarked Place", markers[0].title)
    }

    @Test
    fun testFilterMarkersByLabelsExcludesMonumentsWhenWlmDisabled() {
        val monumentPlace =
            Place().apply {
                name = "Monument Place"
                location = LatLng(10.0, 20.0, 0f)
                isMonument = true
                label = Label.BUILDING
                exists = true
                pic = ""
            }
        NearbyController.markerLabelList.clear()
        NearbyController.markerLabelList.add(MarkerPlaceGroup(false, monumentPlace))

        helper.filterMarkersByLabels(
            listOf(Label.BUILDING),
            filterForPlaceState = false,
            filterForAllNoneType = false,
        )

        val markers = mapView.overlays.filterIsInstance<Marker>().filter { it.title != "Your Location" }
        assertEquals(0, markers.size)
    }

    @Test
    fun testAddPhotoMarkersSinglePhotoInvalidatesMap() {
        isDepictsMode = true
        allPhotoCoords = listOf(LatLng(10.0, 20.0, 0f))

        helper.addPhotoMarkerIfDepictsMode()

        assertNotNull(helper.photoPinBoundingBox)
        val photoMarkers = mapView.overlays.filterIsInstance<Marker>().filter { it.title?.startsWith("Photo") == true }
        assertEquals(1, photoMarkers.size)
    }
}
