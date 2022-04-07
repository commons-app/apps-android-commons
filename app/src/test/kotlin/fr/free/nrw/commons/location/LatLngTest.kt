package fr.free.nrw.commons.location

import fr.free.nrw.commons.location.models.LatLng
import org.junit.Before
import org.junit.Test

class LatLngTest {
    private lateinit var latLng1: LatLng
    private lateinit var latLng2: LatLng

    @Before
    fun setup() {

    }

    @Test
    fun testConstructorSmallLongitude() {
        latLng1 =
            LatLng(0.0, -181.0, 0.0f)
        assert(latLng1.longitude == 179.0)
    }

    @Test
    fun testConstructorBigLongitude() {
        latLng1 =
            LatLng(0.0, 181.0, 0.0f)
        assert(latLng1.longitude == -179.0)
    }

    @Test
    fun testConstructorSmallLatitude() {
        latLng1 =
            LatLng(-91.0, 0.0, 0.0f)
        assert(latLng1.latitude == -90.0)
    }

    @Test
    fun testConstructorBigLatitude() {
        latLng1 =
            LatLng(91.0, 0.0, 0.0f)
        assert(latLng1.latitude == 90.0)
    }

    @Test
    fun testHashCodeDiffersWenLngZero() {
        latLng1 = LatLng(2.0, 0.0, 0.0f)
        latLng2 = LatLng(1.0, 0.0, 0.0f)
        assert(latLng1.hashCode()!=latLng2.hashCode())
    }

    @Test
    fun testHashCodeDiffersWenLatZero() {
        latLng1 = LatLng(0.0, 1.0, 0.0f)
        latLng2 = LatLng(0.0, 2.0, 0.0f)
        assert(latLng1.hashCode()!=latLng2.hashCode())
    }

    @Test
    fun testEqualsWorks() {
        latLng1 = LatLng(1.0, 2.0, 5.0f)
        latLng2 = LatLng(1.0, 2.0, 0.0f)
        assert(latLng1.equals(latLng2))
    }

    @Test
    fun testToString() {
        latLng1 = LatLng(1.0, 2.0, 5.0f)
        assert(latLng1.toString().equals("lat/lng: (1.0,2.0)"))
    }
}