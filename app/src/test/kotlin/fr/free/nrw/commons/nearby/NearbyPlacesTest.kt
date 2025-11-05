package fr.free.nrw.commons.nearby

import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class NearbyPlacesTest {
    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    private lateinit var currentLatLong: LatLng

    private lateinit var nearbyPlaces: NearbyPlaces

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        nearbyPlaces = NearbyPlaces(okHttpJsonApiClient)
    }

    @Test
    fun testRadiusExpander() {
        nearbyPlaces.radiusExpander(currentLatLong, "test", true, "test")
        verify(okHttpJsonApiClient, times(5)).getNearbyPlaces(
            eq(currentLatLong),
            eq("test"),
            any(),
            eq("test"),
        )
    }
}
