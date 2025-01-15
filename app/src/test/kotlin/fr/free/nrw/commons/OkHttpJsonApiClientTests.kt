package fr.free.nrw.commons

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.times
import org.mockito.MockitoAnnotations
import java.lang.Exception

class OkHttpJsonApiClientTests {
    @Mock
    lateinit var okhttpClient: OkHttpClient

    @Mock
    lateinit var depictsClient: DepictsClient

    @Mock
    lateinit var wikiMediaToolforgeUrl: HttpUrl

    var sparqlQueryUrl: String = "https://www.testqparql.com"
    var campaignsUrl: String = "https://www.testcampaignsurl.com"

    @Mock
    lateinit var gson: Gson

    @Mock
    lateinit var latLng: LatLng
    private lateinit var okHttpJsonApiClient: OkHttpJsonApiClient

    @Mock
    lateinit var call: Call

    @Mock
    lateinit var response: Response

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        okHttpJsonApiClient =
            OkHttpJsonApiClient(
                okhttpClient,
                depictsClient,
                wikiMediaToolforgeUrl,
                sparqlQueryUrl,
                campaignsUrl,
                gson,
            )
        Mockito.`when`(okhttpClient.newCall(any())).thenReturn(call)
        Mockito.`when`(call.execute()).thenReturn(response)
    }

    @Test
    fun testGetNearbyPlacesCustomQuery() {
        Mockito.`when`(response.message).thenReturn("test")
        try {
            okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, "test")
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(NearbyQueryParams.Rectangular(latLng, latLng), "test", true, "test")
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        verify(okhttpClient, times(2)).newCall(any())
        verify(call, times(2)).execute()
    }

    @Test
    fun testGetNearbyPlaces() {
        Mockito.`when`(response.message).thenReturn("test")
        try {
            okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, null)
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(
                NearbyQueryParams.Rectangular(latLng, latLng),
                "test",
                true,
                null
            )

        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(
                NearbyQueryParams.Radial(latLng, 10f),
                "test",
                true,
                null
            )
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        verify(okhttpClient, times(3)).newCall(any())
        verify(call, times(3)).execute()
    }

    @Test
    fun testGetNearbyItemCount() {
        Mockito.`when`(response.message).thenReturn("test")
        try {
            okHttpJsonApiClient.getNearbyItemCount(NearbyQueryParams.Radial(latLng, 10f))
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        try {
            okHttpJsonApiClient.getNearbyItemCount(NearbyQueryParams.Rectangular(latLng, latLng))
        } catch (e: Exception) {
            assert(e.message.equals("test"))
        }
        verify(okhttpClient, times(2)).newCall(any())
        verify(call, times(2)).execute()
    }
}
