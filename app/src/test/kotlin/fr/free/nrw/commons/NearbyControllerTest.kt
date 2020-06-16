package fr.free.nrw.commons

import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.NearbyController.loadAttractionsFromLocationToBaseMarkerOptions
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class NearbyControllerTest {

    @Test
    fun testNullAttractions() {
        val location = LatLng(0.0, 0.0, 0f)

        val options = loadAttractionsFromLocationToBaseMarkerOptions(
            location, null, ApplicationProvider.getApplicationContext(), null
        )

        assertEquals(0, options.size.toLong())
    }

    @Test
    fun testEmptyList() {
        val location = LatLng(0.0, 0.0, 0f)

        val options = loadAttractionsFromLocationToBaseMarkerOptions(
            location, emptyList(), ApplicationProvider.getApplicationContext(), emptyList()
        )

        assertEquals(0, options.size.toLong())
    }
}
