package fr.free.nrw.commons

import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController.loadAttractionsFromLocationToBaseMarkerOptions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(constants = BuildConfig::class, sdk = intArrayOf(21), application = TestCommonsApplication::class)
class NearbyControllerTest {

    @Test
    fun testNullAttractions() {
        val location = LatLng(0.0, 0.0, 0f)

        val options = loadAttractionsFromLocationToBaseMarkerOptions(
                location, null, RuntimeEnvironment.application)

        assertEquals(0, options.size.toLong())
    }

    @Test
    fun testEmptyList() {
        val location = LatLng(0.0, 0.0, 0f)

        val options = loadAttractionsFromLocationToBaseMarkerOptions(
                location, emptyList(), RuntimeEnvironment.application)

        assertEquals(0, options.size.toLong())
    }
}
