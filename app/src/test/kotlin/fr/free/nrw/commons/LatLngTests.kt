package fr.free.nrw.commons

import fr.free.nrw.commons.location.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class LatLngTests {
    @Test
    fun testZeroZero() {
        val place = LatLng(0.0, 0.0, 0f)
        assertPrettyCoordinateString("0.0 N, 0.0 E", place)
    }

    @Test
    fun testAntipode() {
        val place = LatLng(0.0, 180.0, 0f)
        assertPrettyCoordinateString("0.0 N, 180.0 W", place)
    }

    @Test
    fun testNorthPole() {
        val place = LatLng(90.0, 0.0, 0f)
        assertPrettyCoordinateString("90.0 N, 0.0 E", place)
    }

    @Test
    fun testSouthPole() {
        val place = LatLng(-90.0, 0.0, 0f)
        assertPrettyCoordinateString("90.0 S, 0.0 E", place)
    }

    @Test
    fun testLargerNumbers() {
        val place = LatLng(120.0, 380.0, 0f)
        assertPrettyCoordinateString("90.0 N, 20.0 E", place)
    }

    @Test
    fun testNegativeNumbers() {
        val place = LatLng(-120.0, -30.0, 0f)
        assertPrettyCoordinateString("90.0 S, 30.0 W", place)
    }

    @Test
    fun testTooBigWestValue() {
        val place = LatLng(20.0, -190.0, 0f)
        assertPrettyCoordinateString("20.0 N, 170.0 E", place)
    }

    @Test
    fun testRounding() {
        val place = LatLng(0.1234567, -0.33333333, 0f)
        assertPrettyCoordinateString("0.1235 N, 0.3333 W", place)
    }

    @Test
    fun testRoundingAgain() {
        val place = LatLng(-0.000001, -0.999999, 0f)
        assertPrettyCoordinateString("0.0 S, 1.0 W", place)
    }

    private fun assertPrettyCoordinateString(expected: String, place: LatLng) =
            assertEquals(expected, place.prettyCoordinateString)
}
