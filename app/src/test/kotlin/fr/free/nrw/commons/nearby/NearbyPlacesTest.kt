package fr.free.nrw.commons.nearby

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argThat
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.mwapi.WikidataQueryClient
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
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
    fun `only needs photo filters the fetch but the area is still sized by all places`() {
        whenever(wikidataQueryClient.getNearbyItemCount(any())).thenReturn(10)

        nearbyPlaces.getFromWikidataQuery(currentLatLong, currentLatLong, currentLatLong, "en", false, null, true)

        verify(wikidataQueryClient).getNearbyItemCount(argThat { !onlyNeedsPhoto })
        verify(okHttpJsonApiClient)
            .getNearbyPlaces(argThat<NearbyQueryParams> { onlyNeedsPhoto }, eq("en"), eq(false), isNull())
    }

    @Test
    fun `places are fetched as before when only needs photo is off`() {
        whenever(wikidataQueryClient.getNearbyItemCount(any())).thenReturn(10)

        nearbyPlaces.getFromWikidataQuery(currentLatLong, currentLatLong, currentLatLong, "en", false, null)

        verify(wikidataQueryClient).getNearbyItemCount(argThat { !onlyNeedsPhoto })
        verify(okHttpJsonApiClient)
            .getNearbyPlaces(argThat<NearbyQueryParams> { !onlyNeedsPhoto }, eq("en"), eq(false), isNull())
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
