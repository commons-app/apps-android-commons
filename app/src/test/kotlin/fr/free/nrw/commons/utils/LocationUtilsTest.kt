package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Test class for location utils
 */
class LocationUtilsTest {

    @Test
    fun testCalculateDistance() {
        val lat1 = 37.7749
        val lon1 = -122.4194
        val lat2 = 34.0522
        val lon2 = -118.2437

        val expectedDistance = 559.02 // Expected distance in kilometers

        val actualDistance = LocationUtils.calculateDistance(lat1, lon1, lat2, lon2)

        assertEquals(expectedDistance, actualDistance, 0.2) // Tolerance = 0.2 km
    }

}