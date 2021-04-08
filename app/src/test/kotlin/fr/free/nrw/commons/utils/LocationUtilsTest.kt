package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import org.junit.Test
import org.junit.jupiter.api.Assertions.assertEquals

/**
 * Test class for location utils
 */
class LocationUtilsTest {

    /**
     * MapBox LatLng to commons LatLng test.
     */
    @Test
    fun testMapBoxLatLngToCommonsLatLng() {
        val commonsLatLngTest = LocationUtils.mapBoxLatLngToCommonsLatLng(com.mapbox.mapboxsdk.geometry.LatLng(0.0, 0.0))
        assertEquals(0.0, commonsLatLngTest.latitude)
        assertEquals(0.0, commonsLatLngTest.longitude)
        assertEquals(0f, commonsLatLngTest.accuracy)
    }

    /**
     * Commons LatLng to MapBox LatLng test.
     */
    @Test
    fun testCommonsLatLngToMapBoxLatLng() {
        val geoLatLngTest = LocationUtils.commonsLatLngToMapBoxLatLng(LatLng(0.0, 0.0, 0f))
        assertEquals(0.0, geoLatLngTest.latitude)
        assertEquals(0.0, geoLatLngTest.longitude)
        assertEquals(0.0, geoLatLngTest.altitude)
    }

}