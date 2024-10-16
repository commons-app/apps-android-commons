package fr.free.nrw.commons.location

import org.junit.Before
import org.junit.Test
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not

class LatLngTest {
    private lateinit var latLng1: LatLng
    private lateinit var latLng2: LatLng

    @Before
    fun setup() {
    }

    @Test
    fun testConstructorSmallLongitude() {
        latLng1 = LatLng(0.0, -181.0, 0.0f)
        assertThat(latLng1.longitude, equalTo(179.0))
    }

    @Test
    fun testConstructorBigLongitude() {
        latLng1 = LatLng(0.0, 181.0, 0.0f)
        assertThat(latLng1.longitude, equalTo(-179.0))
    }

    @Test
    fun testConstructorSmallLatitude() {
        latLng1 = LatLng(-91.0, 0.0, 0.0f)
        assertThat(latLng1.latitude, equalTo(-90.0))
    }

    @Test
    fun testConstructorBigLatitude() {
        latLng1 = LatLng(91.0, 0.0, 0.0f)
        assertThat(latLng1.latitude, equalTo(90.0))
    }

    @Test
    fun testHashCodeDiffersWenLngZero() {
        latLng1 = LatLng(2.0, 0.0, 0.0f)
        latLng2 = LatLng(1.0, 0.0, 0.0f)
        assertThat(latLng1.hashCode(), not(equalTo(latLng2.hashCode())))
    }

    @Test
    fun testHashCodeDiffersWenLatZero() {
        latLng1 = LatLng(0.0, 1.0, 0.0f)
        latLng2 = LatLng(0.0, 2.0, 0.0f)
        assertThat(latLng1.hashCode(), not(equalTo(latLng2.hashCode())))
    }

    @Test
    fun testEqualsWorks() {
        latLng1 = LatLng(1.0, 2.0, 5.0f)
        latLng2 = LatLng(1.0, 2.0, 0.0f)
        assertThat(latLng1, equalTo(latLng2))
    }

    @Test
    fun testToString() {
        latLng1 = LatLng(1.0, 2.0, 5.0f)
        assertThat(latLng1.toString(), equalTo("lat/lng: (1.0,2.0)"))
    }
}
