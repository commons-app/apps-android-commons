package fr.free.nrw.commons.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaceUtilsTest {
    @Test
    fun latLngFromPointStringReadsBlazegraphWkt() {
        val latLng = PlaceUtils.latLngFromPointString("Point(13.3777 52.5163)")

        assertEquals(13.3777, latLng!!.longitude, 0.0)
        assertEquals(52.5163, latLng.latitude, 0.0)
    }

    @Test
    fun latLngFromPointStringReadsQLeverWkt() {
        val latLng = PlaceUtils.latLngFromPointString("POINT(13.377875 52.516714)")

        assertEquals(13.377875, latLng!!.longitude, 0.0)
        assertEquals(52.516714, latLng.latitude, 0.0)
    }

    @Test
    fun latLngFromPointStringReadsPointInsideGlobePrefixedWkt() {
        val latLng = PlaceUtils.latLngFromPointString(
            "<http://www.wikidata.org/entity/Q111> Point(10.5 20.25)"
        )

        assertEquals(10.5, latLng!!.longitude, 0.0)
        assertEquals(20.25, latLng.latitude, 0.0)
    }

    @Test
    fun latLngFromPointStringReturnsNullWhenTextIsNotAPoint() {
        assertNull(PlaceUtils.latLngFromPointString("not a coordinate"))
    }
}
