package fr.free.nrw.commons.utils

import fr.free.nrw.commons.location.LatLng
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo

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
        assertThat(0.0, equalTo( commonsLatLngTest.latitude))
        assertThat(0.0, equalTo( commonsLatLngTest.longitude))
        assertThat(0f, equalTo( commonsLatLngTest.accuracy))
    }

    /**
     * Commons LatLng to MapBox LatLng test.
     */
    @Test
    fun testCommonsLatLngToMapBoxLatLng() {
        val geoLatLngTest = LocationUtils.commonsLatLngToMapBoxLatLng(LatLng(0.0, 0.0, 0f))
        assertThat(0.0, equalTo( geoLatLngTest.latitude))
        assertThat(0.0, equalTo( geoLatLngTest.longitude))
        assertThat(0.0, equalTo( geoLatLngTest.altitude))
    }

}