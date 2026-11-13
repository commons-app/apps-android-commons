package fr.free.nrw.commons.explore.map

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.R
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.databinding.FragmentExploreMapBinding
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.utils.MapUtils.defaultLatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ExploreMapViewportHelperTest {
    private lateinit var helper: ExploreMapViewportHelper
    private lateinit var context: Context
    private lateinit var mapView: MapView
    private lateinit var fabRecenter: FloatingActionButton
    private lateinit var searchThisAreaButton: View
    private val kvStore: JsonKvStore = mock()

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.setTheme(R.style.LightAppTheme)
        val inflater = LayoutInflater.from(context)
        val binding = FragmentExploreMapBinding.inflate(inflater)

        mapView = binding.mapView
        fabRecenter = binding.fabRecenter
        searchThisAreaButton = binding.searchThisAreaButton

        helper = ExploreMapViewportHelper()
        helper.mapView = mapView
        helper.fabRecenter = fabRecenter
        helper.searchThisAreaButton = searchThisAreaButton
        helper.applicationKvStore = kvStore
    }

    /**
     * Verifies default coordinates are returned when kv store has no location.
     */
    @Test
    fun testDefaultMapCenterWhenNoLocationInKvStore() {
        whenever(kvStore.getString("LastLocation")).thenReturn(null)
        val center = helper.getMapCenter()
        assertNotNull(center)
        assertEquals(defaultLatLng.latitude, center!!.latitude, 0.001)
        assertEquals(defaultLatLng.longitude, center.longitude, 0.001)
    }

    /**
     * Verifies map center uses saved coordinates when available in kv store.
     */
    @Test
    fun testMapCenterFromKvStore() {
        whenever(kvStore.getString("LastLocation")).thenReturn("12.34,56.78")
        val center = helper.getMapCenter()
        assertNotNull(center)
        assertEquals(12.34, center!!.latitude, 0.001)
        assertEquals(56.78, center!!.longitude, 0.001)
    }

    /**
     * Verifies explicit mapCenter property overrides kv store.
     */
    @Test
    fun testMapCenterExplicitOverride() {
        helper.mapCenter = GeoPoint(40.0, -74.0)
        val center = helper.getMapCenter()
        assertNotNull(center)
        assertEquals(40.0, center!!.latitude, 0.001)
        assertEquals(-74.0, center!!.longitude, 0.001)
    }

    /**
     * Verifies getMapFocus and getLastMapFocus return coordinates.
     */
    @Test
    fun testGetMapFocusAndLastMapFocus() {
        assertNotNull(helper.getMapFocus())
        assertNotNull(helper.getLastMapFocus())
        helper.lastMapFocus = GeoPoint(15.0, 25.0)
        assertEquals(15.0, helper.getLastMapFocus()!!.latitude, 0.001)
    }

    /**
     * Verifies enabling and disabling recenter FAB button.
     */
    @Test
    fun testEnableAndDisableFabRecenter() {
        helper.disableFABRecenter()
        assertFalse(fabRecenter.isEnabled)
        helper.enableFABRecenter()
        assertTrue(fabRecenter.isEnabled)
    }

    /**
     * Verifies search this area button visibility changes.
     */
    @Test
    fun testSearchThisAreaButtonVisibility() {
        helper.setSearchThisAreaButtonVisibility(true)
        assertTrue(helper.isSearchThisAreaButtonVisible())
        helper.setSearchThisAreaButtonVisibility(false)
        assertFalse(helper.isSearchThisAreaButtonVisible())
    }

    /**
     * Verifies recenterMap with null coordinates marks recenterToUserLocation.
     */
    @Test
    fun testRecenterMapWithNullLatLng() {
        helper.recenterMap(null)
        assertTrue(helper.recenterToUserLocation)
    }

    /**
     * Verifies recenterMap triggers marker recenter callback with coordinates.
     */
    @Test
    fun testRecenterMapWithCoordinates() {
        var markerRecentered: GeoPoint? = null
        helper.onMarkerRecenter = { markerRecentered = it }
        helper.recenterMap(LatLng(10.0, 20.0, 0f))
        assertNotNull(markerRecentered)
        assertEquals(10.0, markerRecentered!!.latitude, 0.001)
    }

    /**
     * Verifies loadNearbyMapData extracts coordinates and zoom from bundle.
     */
    @Test
    fun testLoadNearbyMapDataWithArguments() {
        val bundle =
            Bundle().apply {
                putDouble("prev_zoom", 14.0)
                putDouble("prev_latitude", 48.8566)
                putDouble("prev_longitude", 2.3522)
            }
        helper.loadNearbyMapData(bundle)
        assertTrue(helper.isCameFromNearbyMap)
        assertEquals(48.8566, helper.prevLatitude, 0.001)
        assertEquals(2.3522, helper.prevLongitude, 0.001)
        assertEquals(14.0, helper.prevZoom, 0.001)
        assertEquals(48.8566, helper.previousLatLng.latitude, 0.001)
    }

    /**
     * Verifies loadNearbyMapData with null bundle leaves state unchanged.
     */
    @Test
    fun testLoadNearbyMapDataWithNullArguments() {
        helper.loadNearbyMapData(null)
        assertFalse(helper.isCameFromNearbyMap)
    }
}
