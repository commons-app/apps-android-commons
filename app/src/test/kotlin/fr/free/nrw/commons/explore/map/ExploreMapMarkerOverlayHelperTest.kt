package fr.free.nrw.commons.explore.map

import android.content.Context
import android.view.LayoutInflater
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.nearby.Place
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.ScaleDiskOverlay
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

/**
 * Unit tests for ExploreMapMarkerOverlayHelper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreMapMarkerOverlayHelperTest {
    private lateinit var helper: ExploreMapMarkerOverlayHelper
    private lateinit var mapView: MapView
    private lateinit var context: Context
    private lateinit var detailsSheetHelper: ExploreMapDetailsSheetHelper

    /**
     * Initializes test dependencies before each test.
     */
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.LightAppTheme)
        val inflater = LayoutInflater.from(context)
        val binding = FragmentExploreMapBinding.inflate(inflater)
        mapView = binding.mapView
        detailsSheetHelper = ExploreMapDetailsSheetHelper()
        detailsSheetHelper.init(binding.bottomSheetDetailsBinding)

        helper = ExploreMapMarkerOverlayHelper()
        helper.init(
            mapView = mapView,
            getContext = { context },
            isAttached = { true },
            detailsSheetHelper = detailsSheetHelper,
            onMarkerClicked = {},
        )
    }

    /**
     * Verifies getMediaFromImageURL returns null for null url or null list.
     */
    @Test
    fun testGetMediaFromImageURLWithNulls() {
        assertNull(helper.getMediaFromImageURL(null, null))
        assertNull(helper.getMediaFromImageURL("https://example.com/pic.jpg", null))
        assertNull(helper.getMediaFromImageURL(null, emptyList()))
    }

    /**
     * Verifies getMediaFromImageURL returns matching media item.
     */
    @Test
    fun testGetMediaFromImageURLMatching() {
        val media: Media = mock()
        whenever(media.imageUrl).thenReturn("https://example.com/pic.jpg")
        val result = helper.getMediaFromImageURL("https://example.com/pic.jpg", listOf(media))
        assertSame(media, result)
    }

    /**
     * Verifies getMediaFromImageURL returns null when no matching url is found.
     */
    @Test
    fun testGetMediaFromImageURLNonMatching() {
        val media: Media = mock()
        whenever(media.imageUrl).thenReturn("https://example.com/pic1.jpg")
        val result = helper.getMediaFromImageURL("https://example.com/pic2.jpg", listOf(media))
        assertNull(result)
    }

    /**
     * Verifies getOverlay returns null when item is null.
     */
    @Test
    fun testGetOverlayWithNullItem() {
        assertNull(helper.getOverlay(null))
    }

    /**
     * Verifies getOverlay finds overlay containing target item.
     */
    @Test
    fun testGetOverlayFindsItem() {
        val item = OverlayItem("title", "snippet", GeoPoint(10.0, 20.0))
        val overlay =
            ItemizedOverlayWithFocus<OverlayItem>(
                listOf(item),
                null,
                context,
            )
        mapView.overlays.add(overlay)
        val found = helper.getOverlay(item)
        assertSame(overlay, found)
    }

    /**
     * Verifies removeMarker ignores null place or non-existent marker safely.
     */
    @Test
    fun testRemoveMarkerNonExistent() {
        val place = Place().apply { name = "Unknown" }
        val marker = BaseMarker().apply { this.place = place }
        helper.removeMarker(marker)
        helper.removeMarker(null)
        assertNotNull(helper)
    }

    /**
     * Verifies drawMyLocationMarker adds location marker to overlays.
     */
    @Test
    fun testDrawMyLocationMarker() {
        val location =
            fr.free.nrw.commons.location
                .LatLng(12.34, 56.78, 0f)
        val initialCount = mapView.overlays.size
        helper.drawMyLocationMarker(location)
        assertEquals(initialCount + 1, mapView.overlays.size)
        val addedMarker = mapView.overlays.last() as Marker
        assertEquals("Your Location", addedMarker.title)
    }

    /**
     * Verifies recenterMarkerToPosition updates center and replaces scale disk.
     */
    @Test
    fun testRecenterMarkerToPosition() {
        val geoPoint = GeoPoint(10.0, 20.0)
        helper.recenterMarkerToPosition(geoPoint)

        val hasDisk = mapView.overlays.any { it is ScaleDiskOverlay }
        val hasMarker = mapView.overlays.any { it is Marker && it.title == "Your Location" }
        assertTrue(hasDisk)
        assertTrue(hasMarker)
    }

    /**
     * Verifies removeMarker removes overlay and entry from map.
     */
    @Test
    fun testRemoveMarker() {
        val place =
            Place().apply {
                name = "File:Test.jpg"
                location =
                    fr.free.nrw.commons.location
                        .LatLng(1.0, 2.0, 0f)
            }
        val baseMarker = BaseMarker()
        baseMarker.place = place
        val overlay: Overlay = mock()
        mapView.overlays.add(overlay)
        helper.baseMarkerOverlayMap = mutableMapOf(baseMarker to overlay)

        helper.removeMarker(baseMarker)
        assertTrue(helper.baseMarkerOverlayMap?.containsKey(baseMarker) == false)
    }

    /**
     * Verifies clearAllMarkers clears overlay manager and handles null center safely.
     */
    @Test
    fun testClearAllMarkers() {
        helper.clearAllMarkers()
        assertNotNull(mapView.overlays)
    }

    /**
     * Verifies clearAllMarkers adds scale disk overlay when mapCenter is provided.
     */
    @Test
    fun testClearAllMarkersWithMapCenter() {
        helper.mapCenterProvider = { GeoPoint(10.0, 20.0) }
        helper.clearAllMarkers()
        assertTrue(mapView.overlays.any { it is ScaleDiskOverlay })
    }

    /**
     * Verifies addMarkersToMap handles empty or null marker lists.
     */
    @Test
    fun testAddMarkersToMap() {
        helper.addMarkersToMap(null)
        helper.addMarkersToMap(emptyList())
        assertNotNull(mapView.overlays)
    }
}
