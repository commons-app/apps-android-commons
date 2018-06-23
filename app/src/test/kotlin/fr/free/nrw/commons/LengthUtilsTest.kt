package fr.free.nrw.commons

import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.utils.LengthUtils
import org.junit.Assert.assertEquals
import org.junit.Test

class LengthUtilsTest {
    @Test
    fun testZeroDistance() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 0.0, 0f)
        assertDistanceBetween("0m", pointA, pointB)
    }

    @Test
    fun testOneDegreeOnEquator() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 1.0, 0f)
        assertDistanceBetween("111.2km", pointA, pointB)
    }

    @Test
    fun testOneDegreeFortyFiveDegrees() {
        val pointA = LatLng(45.0, 0.0, 0f)
        val pointB = LatLng(45.0, 1.0, 0f)
        assertDistanceBetween("78.6km", pointA, pointB)
    }

    @Test
    fun testOneDegreeSouthPole() {
        val pointA = LatLng(-90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 1.0, 0f)
        assertDistanceBetween("0m", pointA, pointB)
    }

    @Test
    fun testPoleToPole() {
        val pointA = LatLng(90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 0.0, 0f)
        assertDistanceBetween("20,015.1km", pointA, pointB)
    }

    private fun assertDistanceBetween(expected: String, pointA: LatLng, pointB: LatLng) =
            assertEquals(expected, LengthUtils.formatDistanceBetween(pointA, pointB))
}
