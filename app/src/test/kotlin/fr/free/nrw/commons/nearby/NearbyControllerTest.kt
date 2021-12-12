package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.eq
import fr.free.nrw.commons.location.LatLng
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.util.*

class NearbyControllerTest {
    @Mock
    private lateinit var nearbyPlaces: NearbyPlaces

    @Mock
    lateinit var searchLatLong: LatLng

    @Mock
    lateinit var currentLatLng: LatLng
    var customQuery: String = "test"
    private lateinit var nearbyController: NearbyController

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        nearbyController = NearbyController(nearbyPlaces)
    }

    @Test
    fun testLoadAttractionsForLocationTest() {
        Mockito.`when`(nearbyPlaces.radiusExpander(any(), any(), any(), any(), any()))
            .thenReturn(Collections.emptyList())
        nearbyController.loadAttractionsFromLocation(
            searchLatLong,
            currentLatLng,
            false,
            false,
            true,
            customQuery
        )
        Mockito.verify(nearbyPlaces).radiusExpander(
            eq(currentLatLng),
            any(String::class.java),
            eq(false),
            eq(true),
            eq(customQuery)
        )
    }

    @Test
    fun testLoadAttractionsForLocationTestNoQuery() {
        Mockito.`when`(nearbyPlaces.radiusExpander(any(), any(), any(), any(), anyOrNull()))
            .thenReturn(Collections.emptyList())
        nearbyController.loadAttractionsFromLocation(
            searchLatLong,
            currentLatLng,
            false,
            false,
            true
        )
        Mockito.verify(nearbyPlaces).radiusExpander(
            eq(currentLatLng),
            any(String::class.java),
            eq(false),
            eq(true),
            eq(null)
        )
    }

    fun <T> any(type: Class<T>): T = Mockito.any<T>(type)
}