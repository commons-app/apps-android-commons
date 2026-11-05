package fr.free.nrw.commons

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.times
import fr.free.nrw.commons.campaigns.CampaignResponseDTO
import fr.free.nrw.commons.campaigns.CampaignConfig
import fr.free.nrw.commons.campaigns.models.Campaign
import fr.free.nrw.commons.explore.depictions.DepictsClient
import fr.free.nrw.commons.location.LatLng
import com.google.gson.JsonSyntaxException
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient
import fr.free.nrw.commons.mwapi.TooManyAchievementsException
import fr.free.nrw.commons.nearby.model.NearbyQueryParams
import okhttp3.Call
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.mockwebserver.MockResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.eq
import org.mockito.MockitoAnnotations
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
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

    @Mock
    lateinit var responseBody: ResponseBody

    private lateinit var mockWebServer: TestWebServer

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        mockWebServer = TestWebServer()
        mockWebServer.setUp()
        okHttpJsonApiClient = OkHttpJsonApiClient(
            okhttpClient,
            depictsClient,
            wikiMediaToolforgeUrl,
            sparqlQueryUrl,
            mockWebServer.getUrl(), //use the mock server for the campaignsUrl
            gson
        )
        Mockito.`when`(okhttpClient.newCall(any())).thenReturn(call)
        Mockito.`when`(call.execute()).thenReturn(response)
        Mockito.`when`(response.isSuccessful).thenReturn(false)
        Mockito.`when`(response.message).thenReturn("test")
        Mockito.`when`(response.body).thenReturn(responseBody)
        Mockito.`when`(responseBody.string()).thenReturn("{\"error\": \"test\"}")
    }

    @Test
    fun testGetNearbyPlacesCustomQuery() {
        try {
            okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, "test")
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(NearbyQueryParams.Rectangular(latLng, latLng), "test", true, "test")
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        verify(okhttpClient, times(2)).newCall(any())
        verify(call, times(2)).execute()
    }

    @Test
    fun testGetNearbyPlaces() {
        try {
            okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, null)
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(
                NearbyQueryParams.Rectangular(latLng, latLng),
                "test",
                true,
                null
            )
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        try {
            okHttpJsonApiClient.getNearbyPlaces(
                NearbyQueryParams.Radial(latLng, 10f),
                "test",
                true,
                null
            )
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        verify(okhttpClient, times(3)).newCall(any())
        verify(call, times(3)).execute()
    }

    @Test
    fun testGetNearbyItemCount() {
        try {
            okHttpJsonApiClient.getNearbyItemCount(NearbyQueryParams.Radial(latLng, 10f))
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        try {
            okHttpJsonApiClient.getNearbyItemCount(NearbyQueryParams.Rectangular(latLng, latLng))
        } catch (e: Exception) {
            assertEquals("test", e.message)
        }
        verify(okhttpClient, times(2)).newCall(any())
        verify(call, times(2)).execute()
    }

    @Test
    fun testGetCampaignsWithData() {
        //loads the json response from resources
        val jsonResponse = loadJsonFromResource("campaigns_response_with_data.json")

        //mocks the succesfull response chain
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(response.message).thenReturn("OK")
        Mockito.`when`(response.body).thenReturn(responseBody)
        Mockito.`when`(responseBody.string()).thenReturn(jsonResponse)

        val campaignResponse = CampaignResponseDTO().apply {
            campaignConfig = CampaignConfig().apply {
                showOnlyLiveCampaigns = false
                sortBy = "startDate"
            }
            campaigns = listOf(
                Campaign().apply {
                    title = "Wiki Loves Monuments"
                    isWLMCampaign = true
                },
                Campaign().apply {
                    title = "Wiki Loves Nature"
                    isWLMCampaign = false
                }
            )
        }

        //any() for the string argument and eq() for the class argument.
        Mockito.`when`(
            gson.fromJson(
                any<String>(),
                eq(CampaignResponseDTO::class.java)
            )
        ).thenReturn(campaignResponse)

        //call the getCampaigns
        val result: CampaignResponseDTO? = okHttpJsonApiClient.getCampaigns().blockingGet()

        //verify the  results
        assertNotNull(result)
        assertNotNull(result?.campaigns)
        assertEquals(2, result?.campaigns!!.size)
        assertEquals("Wiki Loves Monuments", result.campaigns!![0].title)
        assertTrue(result.campaigns!![0].isWLMCampaign)
        assertEquals("Wiki Loves Nature", result.campaigns!![1].title)
        assertEquals(false, result.campaigns!![1].isWLMCampaign)
        assertNotNull(result.campaignConfig)
        assertFalse(result.campaignConfig!!.showOnlyLiveCampaigns)
        assertEquals("startDate", result.campaignConfig!!.sortBy)
    }

    @Test
    fun testGetCampaignsEmpty() {
        //loads the empty json response
        val jsonResponse = loadJsonFromResource("campaigns_response_empty.json")

        //mocks the successful response chain
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(response.message).thenReturn("OK")
        Mockito.`when`(response.body).thenReturn(responseBody)
        Mockito.`when`(responseBody.string()).thenReturn(jsonResponse)

        val campaignResponse = CampaignResponseDTO().apply {
            campaignConfig = CampaignConfig().apply {
                showOnlyLiveCampaigns = false
                sortBy = "startDate"
            }
            campaigns = emptyList()
        }

        //use any() for the string argument and eq() for the class argument.
        Mockito.`when`(
            gson.fromJson(
                any<String>(),
                eq(CampaignResponseDTO::class.java)
            )
        ).thenReturn(campaignResponse)

        //calls getCampaigns
        val result: CampaignResponseDTO? = okHttpJsonApiClient.getCampaigns().blockingGet()

        //verify the results
        assertNotNull(result)
        assertNotNull(result?.campaigns)
        assertTrue(result?.campaigns!!.isEmpty())
        assertNotNull(result.campaignConfig)
        assertFalse(result.campaignConfig!!.showOnlyLiveCampaigns)
        assertEquals("startDate", result.campaignConfig!!.sortBy)
    }

    @Test
    fun testGetAchievementsFallsBackToCommonsApiWhenFeedbackHasNoJson() {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn(
            "Content-type: application/json\n",
            """{"query":{"logevents":[{"title":"File:A.jpg"},{"title":"File:B.jpg"}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg"},{"title":"File:B.jpg","missing":true}]}}""",
            """{"continue":{"gucontinue":"x","continue":"gaicontinue||"},"query":{"pages":[""" +
                """{"pageid":1,"title":"File:A.jpg","globalusage":[{"title":"P1","wiki":"en.wikipedia.org"}],""" +
                """"categories":[{"ns":14,"title":"Category:Quality_images"}]}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg",""" +
                """"globalusage":[{"title":"P2","wiki":"fr.wikipedia.org"}]}]}}""",
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}"""
        )

        val result = clientWithToolforgeUrl().getAchievements("Test").blockingGet()!!

        assertEquals(1, result.uniqueUsedImages)
        assertEquals(2, result.articlesUsingImages)
        assertEquals(1, result.deletedUploads)
        assertEquals(1, result.featuredImages.qualityImages)
        assertEquals(0, result.featuredImages.featuredPicturesOnWikimediaCommons)
        assertEquals(3, result.thanksReceived)
        assertEquals("Test", result.user)
        verify(okhttpClient, times(6)).newCall(any())
    }

    @Test
    fun testGetAchievementsFallbackFailsForTooManyUploads() {
        val uploads = (1..1001).joinToString(",") { """{"title":"File:$it.jpg"}""" }
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn(
            "Content-type: application/json\n",
            """{"query":{"logevents":[$uploads]}}"""
        )

        try {
            clientWithToolforgeUrl().getAchievements("Test").blockingGet()
            fail("Expected the fallback to give up")
        } catch (e: RuntimeException) {
            assertTrue(e.cause is TooManyAchievementsException)
        }
        verify(okhttpClient, times(2)).newCall(any())
    }

    @Test
    fun testGetAchievementsFallsBackWhenFeedbackIsNotSuccessful() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        stubCommonsAchievementsResponses()

        val result = clientWithToolforgeUrl().getAchievements("Test").blockingGet()!!

        assertEquals(1, result.uniqueUsedImages)
        assertEquals(3, result.thanksReceived)
        verify(okhttpClient, times(6)).newCall(any())
    }

    @Test
    fun testGetAchievementsFallsBackWhenFeedbackThrows() {
        Mockito.`when`(call.execute())
            .thenThrow(IOException("no healthy upstream"))
            .thenReturn(response)
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        stubCommonsAchievementsResponses()

        val result = clientWithToolforgeUrl().getAchievements("Test").blockingGet()!!

        assertEquals(1, result.deletedUploads)
        assertEquals("Test", result.user)
    }

    @Test
    fun testGetAchievementsFallsBackWhenFeedbackJsonIsInvalid() {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(
            gson.fromJson(any<String>(), eq(fr.free.nrw.commons.profile.achievements.FeedbackResponse::class.java))
        ).thenThrow(JsonSyntaxException("bad"))
        Mockito.`when`(responseBody.string()).thenReturn(
            "{not json",
            """{"query":{"logevents":[{"title":"File:A.jpg"},{"title":"File:B.jpg"}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg"},{"title":"File:B.jpg","missing":true}]}}""",
            """{"continue":{"gucontinue":"x","continue":"gaicontinue||"},"query":{"pages":[""" +
                """{"pageid":1,"title":"File:A.jpg","globalusage":[{"title":"P1","wiki":"en.wikipedia.org"}],""" +
                """"categories":[{"ns":14,"title":"Category:Quality_images"}]}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg",""" +
                """"globalusage":[{"title":"P2","wiki":"fr.wikipedia.org"}]}]}}""",
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}"""
        )

        val result = clientWithToolforgeUrl().getAchievements("Test").blockingGet()!!

        assertEquals(2, result.articlesUsingImages)
    }

    @Test
    fun testGetUploadCountFallsBackWhenToolforgeFails() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"allimages":[{"name":"A.jpg"},{"name":"B.jpg"}]}}"""
        )

        val count = clientWithToolforgeUrl().getUploadCount("Test").blockingGet()

        assertEquals(2, count)
    }

    @Test
    fun testGetUploadCountFallbackStopsAfterTheCap() {
        val page = (1..500).joinToString(",") { """{"name":"F$it.jpg"}""" }
        val continued = """{"continue":{"aicontinue":"x","continue":"-||"},"query":{"allimages":[$page]}}"""
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            continued,
            continued,
            """{"query":{"allimages":[$page]}}"""
        )

        val count = clientWithToolforgeUrl().getUploadCount("Test").blockingGet()

        assertEquals(1000, count)
        verify(okhttpClient, times(3)).newCall(any())
    }

    @Test
    fun testGetWikidataEditsFallsBackWhenToolforgeFails() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"users":[{"name":"Test","editcount":12}]}}"""
        )

        val edits = clientWithToolforgeUrl().getWikidataEdits("Test").blockingGet()

        assertEquals(12, edits)
    }

    private fun stubCommonsAchievementsResponses() {
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"logevents":[{"title":"File:A.jpg"},{"title":"File:B.jpg"}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg"},{"title":"File:B.jpg","missing":true}]}}""",
            """{"continue":{"gucontinue":"x","continue":"gaicontinue||"},"query":{"pages":[""" +
                """{"pageid":1,"title":"File:A.jpg","globalusage":[{"title":"P1","wiki":"en.wikipedia.org"}],""" +
                """"categories":[{"ns":14,"title":"Category:Quality_images"}]}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg",""" +
                """"globalusage":[{"title":"P2","wiki":"fr.wikipedia.org"}]}]}}""",
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}"""
        )
    }

    private fun clientWithToolforgeUrl() = OkHttpJsonApiClient(
        okhttpClient,
        depictsClient,
        "https://tools.example.org/tool-commons-android-app".toHttpUrl(),
        sparqlQueryUrl,
        campaignsUrl,
        gson
    )

    fun loadJsonFromResource(fileName: String): String {
        val resourcePath = "raw/$fileName"
        //uses the classloader to find the resource in the test environment
        val inputStream = javaClass.classLoader?.getResourceAsStream(resourcePath)

        if (inputStream != null) {
            //reads the entire stream content
            return BufferedReader(InputStreamReader(inputStream)).use { it.readText() }
        }
        //throws an exception with the correct expected path
        throw IllegalArgumentException(
            "Resource $fileName not found. Please ensure the file is located " +
                "in app/src/test/resources/raw/",
        )
    }

    /**
     * Test that an unsuccessful response is reported as an IOException
     */
    @Test
    fun testNearbyQueriesThrowIOExceptionOnUnsuccessfulResponse() {
        val queries =
            listOf<() -> Any?>(
                { okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, "test") },
                { okHttpJsonApiClient.getNearbyPlaces(latLng, "test", 10.0, null) },
                { okHttpJsonApiClient.getNearbyItemCount(NearbyQueryParams.Radial(latLng, 10f)) },
            )

        queries.forEach { query ->
            val exception = assertThrows(IOException::class.java) { query() }
            assertEquals("test", exception.message)
        }
    }
}
