package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations

class NearbyPlacesTest {
    @Mock
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    private lateinit var wikidataQueryClient: WikidataQueryClient

    @Mock
    private lateinit var currentLatLong: LatLng

    private lateinit var nearbyPlaces: NearbyPlaces

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        nearbyPlaces = NearbyPlaces(okHttpJsonApiClient, wikidataQueryClient)
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
