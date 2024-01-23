package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LengthUtilsTest {
    // Test LengthUtils.formatDistanceBetween()

    @Test
    fun testFormattedDistanceBetweenSamePoints() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 0.0, 0f)
        assertFormattedDistanceBetween("0m", pointA, pointB)
    }

    @Test
    fun testFormattedOneDegreeOnEquator() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 1.0, 0f)
        assertFormattedDistanceBetween("111.2km", pointA, pointB)
    }

    @Test
    fun testFormattedOneDegreeFortyFiveDegrees() {
        val pointA = LatLng(45.0, 0.0, 0f)
        val pointB = LatLng(45.0, 1.0, 0f)
        assertFormattedDistanceBetween("78.6km", pointA, pointB)
    }

    @Test
    fun testFormattedOneDegreeSouthPole() {
        val pointA = LatLng(-90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 1.0, 0f)
        assertFormattedDistanceBetween("0m", pointA, pointB)
    }

    @Test
    fun testFormattedPoleToPole() {
        val pointA = LatLng(90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 0.0, 0f)
        assertFormattedDistanceBetween("20,015.1km", pointA, pointB)
    }

    @Test
    fun testFormattedNullToNull() {
        assertNull(LengthUtils.formatDistanceBetween(null, null))
    }

    // Test LengthUtils.formatDistance()

    @Test
    fun testFormatDistance() {
        assertFormattedDistance("100m", 100)
        assertFormattedDistance("123m", 123)
        assertFormattedDistance("450m", 450)
        assertFormattedDistance("999m", 999)
        assertFormattedDistance("1km", 1000)
        assertFormattedDistance("1km", 1001)
        assertFormattedDistance("1.1km", 1050)
        assertFormattedDistance("1.2km", 1234)
        assertFormattedDistance("123,456.8km", 123456789)
        assertFormattedDistance("0m", 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testIllegalFormatDistance() {
        LengthUtils.formatDistance(-1)
    }

    // Test LengthUtils.computeDistanceBetween()

    @Test
    fun testDistanceBetweenSamePoints() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 0.0, 0f)
        assertDistanceBetween(0.0, pointA, pointB)
    }

    @Test
    fun testDistanceOneDegreeOnEquator() {
        val pointA = LatLng(0.0, 0.0, 0f)
        val pointB = LatLng(0.0, 1.0, 0f)
        assertDistanceBetween(111195.08, pointA, pointB)
    }

    @Test
    fun testDistanceOneDegreeFortyFiveDegrees() {
        val pointA = LatLng(45.0, 0.0, 0f)
        val pointB = LatLng(45.0, 1.0, 0f)
        assertDistanceBetween(78626.30, pointA, pointB)
    }

    @Test
    fun testDistanceOneDegreeSouthPole() {
        val pointA = LatLng(-90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 1.0, 0f)
        assertDistanceBetween(0.0, pointA, pointB)
    }

    @Test
    fun testDistancePoleToPole() {
        val pointA = LatLng(90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 0.0, 0f)
        assertDistanceBetween(20015115.07, pointA, pointB)
    }

    // Test LengthUtils.formatDistanceBetween()

    @Test
    fun testBearingPoleToPole() {
        val pointA = LatLng(90.0, 0.0, 0f)
        val pointB = LatLng(-90.0, 0.0, 0f)
        assertBearing(180.00, pointA, pointB)
    }

    @Test
    fun testBearingRandomPoints() {
        val pointA = LatLng(27.17, 78.04, 0f)
        val pointB = LatLng(-40.69, 04.13, 0f)
        assertBearing(227.46, pointA, pointB)
    }

    @Test
    fun testBearingSamePlace() {
        val pointA = LatLng(90.0, 0.0, 0f)
        val pointB = LatLng(90.0, 0.0, 0f)
        assertBearing(0.0, pointA, pointB)
    }

    // Test assertion helper functions

    private fun assertFormattedDistanceBetween(expected: String, pointA: LatLng, pointB: LatLng) =
            assertEquals(expected, LengthUtils.formatDistanceBetween(pointA, pointB))

    private fun assertFormattedDistance(expected: String, distance: Int) =
            assertEquals(expected, LengthUtils.formatDistance(distance))

    private fun assertDistanceBetween(expected: Double, pointA: LatLng, pointB: LatLng) =
    // Acceptable error: 1cm
            assertEquals(expected, LengthUtils.computeDistanceBetween(pointA, pointB), 0.01)

    private fun assertBearing(expected: Double, pointA: LatLng, pointB: LatLng) =
        // Acceptable error: 1 degree
        assertEquals(expected, LengthUtils.computeBearing(pointA,pointB), 1.0)
}