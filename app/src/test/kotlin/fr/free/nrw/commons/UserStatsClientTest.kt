package fr.free.nrw.commons

import com.google.gson.Gson
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.mwapi.CommonsUserStats
import fr.free.nrw.commons.mwapi.TooManyAchievementsException
import fr.free.nrw.commons.mwapi.UserStatsClient
import okhttp3.Call
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.ResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.MockitoAnnotations
import java.io.IOException

/** Exercises contributor statistics through the same fallback requests used by the app. */
class UserStatsClientTest {
    @Mock
    private lateinit var okhttpClient: OkHttpClient

    @Mock
    private lateinit var call: Call

    @Mock
    private lateinit var response: Response

    @Mock
    private lateinit var responseBody: ResponseBody

    private lateinit var client: UserStatsClient

    /** Stubs the HTTP transport while keeping the real fallback aggregation. */
    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        client =
            UserStatsClient(
                okhttpClient,
                "https://tools.example.org/tool-commons-android-app".toHttpUrl(),
                Gson(),
                CommonsUserStats(okhttpClient),
            )
        Mockito.`when`(okhttpClient.newCall(any())).thenReturn(call)
        Mockito.`when`(call.execute()).thenReturn(response)
        Mockito.`when`(response.body).thenReturn(responseBody)
    }

    /** Combines Commons pages, usage continuation and thanks when Toolforge has no JSON. */
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
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}""",
        )

        val result = client.getAchievements("Test").blockingGet()!!

        assertEquals(1, result.uniqueUsedImages)
        assertEquals(2, result.articlesUsingImages)
        assertEquals(1, result.deletedUploads)
        assertEquals(1, result.featuredImages.qualityImages)
        assertEquals(0, result.featuredImages.featuredPicturesOnWikimediaCommons)
        assertEquals(3, result.thanksReceived)
        assertEquals("Test", result.user)
        verify(okhttpClient, times(6)).newCall(any())
    }

    /** Stops achievement aggregation when the upload log exceeds the supported size. */
    @Test
    fun testGetAchievementsFallbackFailsForTooManyUploads() {
        val uploads = (1..1001).joinToString(",") { """{"title":"File:$it.jpg"}""" }
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn(
            "Content-type: application/json\n",
            """{"query":{"logevents":[$uploads]}}""",
        )

        try {
            client.getAchievements("Test").blockingGet()
            fail("Expected the fallback to give up")
        } catch (e: RuntimeException) {
            assertTrue(e.cause is TooManyAchievementsException)
        }
        verify(okhttpClient, times(2)).newCall(any())
    }

    /** Computes Commons achievements after an unsuccessful Toolforge response. */
    @Test
    fun testGetAchievementsFallsBackWhenFeedbackIsNotSuccessful() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        stubCommonsAchievementsResponses()

        val result = client.getAchievements("Test").blockingGet()!!

        assertEquals(1, result.uniqueUsedImages)
        assertEquals(3, result.thanksReceived)
        verify(okhttpClient, times(6)).newCall(any())
    }

    /** Keeps achievement results available when the Toolforge request throws. */
    @Test
    fun testGetAchievementsFallsBackWhenFeedbackThrows() {
        Mockito
            .`when`(call.execute())
            .thenThrow(IOException("no healthy upstream"))
            .thenReturn(response)
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        stubCommonsAchievementsResponses()

        val result = client.getAchievements("Test").blockingGet()!!

        assertEquals(1, result.deletedUploads)
        assertEquals("Test", result.user)
    }

    /** Uses Commons when Toolforge returns malformed JSON. */
    @Test
    fun testGetAchievementsFallsBackWhenFeedbackJsonIsInvalid() {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn(
            "{not json",
            """{"query":{"logevents":[{"title":"File:A.jpg"},{"title":"File:B.jpg"}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg"},{"title":"File:B.jpg","missing":true}]}}""",
            """{"continue":{"gucontinue":"x","continue":"gaicontinue||"},"query":{"pages":[""" +
                """{"pageid":1,"title":"File:A.jpg","globalusage":[{"title":"P1","wiki":"en.wikipedia.org"}],""" +
                """"categories":[{"ns":14,"title":"Category:Quality_images"}]}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg",""" +
                """"globalusage":[{"title":"P2","wiki":"fr.wikipedia.org"}]}]}}""",
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}""",
        )

        val result = client.getAchievements("Test").blockingGet()!!

        assertEquals(2, result.articlesUsingImages)
    }

    /** Counts files through Commons after a failed Toolforge upload-count request. */
    @Test
    fun testGetUploadCountFallsBackWhenToolforgeFails() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"allimages":[{"name":"A.jpg"},{"name":"B.jpg"}]}}""",
        )

        val count = client.getUploadCount("Test").blockingGet()

        assertEquals(2, count)
    }

    /** Stops reading continuation pages once the upload count reaches its cap. */
    @Test
    fun testGetUploadCountFallbackStopsAfterTheCap() {
        val page = (1..500).joinToString(",") { """{"name":"F$it.jpg"}""" }
        val continued = """{"continue":{"aicontinue":"x","continue":"-||"},"query":{"allimages":[$page]}}"""
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            continued,
            continued,
            """{"query":{"allimages":[$page]}}""",
        )

        val count = client.getUploadCount("Test").blockingGet()

        assertEquals(1000, count)
        verify(okhttpClient, times(3)).newCall(any())
    }

    /** Reads the Wikidata user list when Toolforge cannot return the edit count. */
    @Test
    fun testGetWikidataEditsFallsBackWhenToolforgeFails() {
        Mockito.`when`(response.isSuccessful).thenReturn(false, true)
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"users":[{"name":"Test","editcount":12}]}}""",
        )

        val edits = client.getWikidataEdits("Test").blockingGet()

        assertEquals(12, edits)
    }

    /** A valid Toolforge edit count avoids a second request to the Wikidata API. */
    @Test
    fun usesToolforgeEditCountWhenPresent() {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn("Content-type: application/json\n{\"edits\":9}")

        assertEquals(9, client.getWikidataEdits("Test").blockingGet())
        verify(okhttpClient).newCall(any())
    }

    /** A nonnumeric Toolforge edit count still falls back to the Wikidata user list. */
    @Test
    fun fallsBackWhenToolforgeEditCountIsMalformed() {
        Mockito.`when`(response.isSuccessful).thenReturn(true)
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"edits":"invalid"}""",
            """{"query":{"users":[{"editcount":12}]}}""",
        )

        assertEquals(12, client.getWikidataEdits("Test").blockingGet())
        verify(okhttpClient, times(2)).newCall(any())
    }

    /** Supplies the response sequence for an achievement fallback with continued file usage. */
    private fun stubCommonsAchievementsResponses() {
        Mockito.`when`(responseBody.string()).thenReturn(
            """{"query":{"logevents":[{"title":"File:A.jpg"},{"title":"File:B.jpg"}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg"},{"title":"File:B.jpg","missing":true}]}}""",
            """{"continue":{"gucontinue":"x","continue":"gaicontinue||"},"query":{"pages":[""" +
                """{"pageid":1,"title":"File:A.jpg","globalusage":[{"title":"P1","wiki":"en.wikipedia.org"}],""" +
                """"categories":[{"ns":14,"title":"Category:Quality_images"}]}]}}""",
            """{"query":{"pages":[{"pageid":1,"title":"File:A.jpg",""" +
                """"globalusage":[{"title":"P2","wiki":"fr.wikipedia.org"}]}]}}""",
            """{"query":{"logevents":[{"logid":1},{"logid":2},{"logid":3}]}}""",
        )
    }
}
