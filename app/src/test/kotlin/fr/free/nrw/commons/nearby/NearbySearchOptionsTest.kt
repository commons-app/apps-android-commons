package fr.free.nrw.commons.nearby

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NearbySearchOptionsTest {
    @Test
    fun emptyOptionsProduceNoFilter() {
        assertEquals("", NearbySearchOptions().toSparql())
    }

    @Test
    fun placeTypesUseDirectInstanceOf() {
        val options =
            NearbySearchOptions(
                null,
                listOf("Q16970", "Q3947"),
                emptyList(),
                false,
                false,
                false,
            )
        assertEquals(
            "?item wdt:P31 ?classFilter .\nVALUES ?classFilter { wd:Q16970 wd:Q3947 }",
            options.toSparql(),
        )
    }

    @Test
    fun subclassesUseASingleExtraHop() {
        val sparql =
            NearbySearchOptions(
                null,
                listOf("Q16970"),
                emptyList(),
                true,
                false,
                false,
            ).toSparql()
        assertTrue(sparql.contains("wdt:P31/wdt:P279?"))
        assertFalse(sparql.contains("wdt:P279*"))
    }

    @Test
    fun duplicateIdsAreEmittedOnce() {
        val sparql =
            NearbySearchOptions(
                null,
                listOf("Q1", "Q1"),
                emptyList(),
                false,
                false,
                false,
            ).toSparql()
        assertEquals(
            "?item wdt:P31 ?classFilter .\nVALUES ?classFilter { wd:Q1 }",
            sparql,
        )
    }

    @Test
    fun missingImageAndDestroyedFiltersUseFreshVariables() {
        val sparql =
            NearbySearchOptions(
                null,
                emptyList(),
                emptyList(),
                false,
                true,
                true,
            ).toSparql()
        assertTrue(sparql.contains("FILTER NOT EXISTS { ?item wdt:P18 ?picFilter }"))
        assertTrue(sparql.contains("FILTER NOT EXISTS { ?item wdt:P576 ?destroyedFilter }"))
        assertTrue(sparql.contains("FILTER NOT EXISTS { ?item wdt:P582 ?endTimeFilter }"))
    }

    @Test
    fun subclassFlagWithoutTypesAddsNothing() {
        val options =
            NearbySearchOptions(
                null,
                emptyList(),
                emptyList(),
                true,
                false,
                false,
            )
        assertEquals("", options.toSparql())
    }

    @Test
    fun sliderStartsOnTheCurrentMapView() {
        assertEquals(null, NearbySearchOptions.radiusKmForSlider(0))
        assertEquals(0, NearbySearchOptions.sliderProgressForRadius(null))
    }

    @Test
    fun sliderLandsOnLabeledDistancesAndValuesBetweenThem() {
        assertEquals(1.0, NearbySearchOptions.radiusKmForSlider(100)!!, 0.001)
        assertEquals(5.0, NearbySearchOptions.radiusKmForSlider(200)!!, 0.001)
        assertEquals(10.0, NearbySearchOptions.radiusKmForSlider(300)!!, 0.001)
        assertEquals(
            50.0,
            NearbySearchOptions.radiusKmForSlider(NearbySearchOptions.RADIUS_SLIDER_MAX)!!,
            0.001,
        )
        assertEquals(3.0, NearbySearchOptions.radiusKmForSlider(150)!!, 0.001)
        assertEquals(300, NearbySearchOptions.sliderProgressForRadius(10.0))
        assertEquals(
            10.0,
            NearbySearchOptions.radiusKmForSlider(NearbySearchOptions.sliderProgressForRadius(10.0))!!,
            0.001,
        )
    }

    @Test
    fun invalidIdsAreRejected() {
        for (id in listOf("q16970", "Q", "Q1a", "16970", "")) {
            val options =
                NearbySearchOptions(
                    null,
                    listOf(id),
                    emptyList(),
                    false,
                    false,
                    false,
                )
            assertThrows(IllegalArgumentException::class.java) { options.toSparql() }
        }
    }
}
