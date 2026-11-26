package fr.free.nrw.commons.delete

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.verifyNoMoreInteractions
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.Media
import fr.free.nrw.commons.actions.PageEditClient
import fr.free.nrw.commons.wikidata.GsonUtil.defaultGson
import io.reactivex.Observable
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Checks the G7 speedy deletion rules: the upload must be less than 7 days old and must not be
 * used anywhere, and only the file page is edited. The usage check runs against a local web
 * server with real Commons responses.
 */
class SpeedyDeletionHelperTest {
    private lateinit var server: MockWebServer
    private lateinit var pageEditClient: PageEditClient
    private lateinit var helper: SpeedyDeletionHelper

    private val media = Media(filename = "File:Example upload.jpg")

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val speedyDeletionInterface =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .addConverterFactory(GsonConverterFactory.create(defaultGson))
                .build()
                .create(SpeedyDeletionInterface::class.java)
        pageEditClient = mock()
        whenever(pageEditClient.prependEdit(any(), any(), any())).thenReturn(Observable.just(true))
        helper = SpeedyDeletionHelper(speedyDeletionInterface, pageEditClient)
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun fixture(name: String): String =
        javaClass.classLoader!!
            .getResourceAsStream("speedy_deletion/$name")!!
            .bufferedReader()
            .use { it.readText() }

    private fun enqueue(body: String) {
        server.enqueue(MockResponse().setBody(body))
    }

    private fun page(fields: String) = """{"query":{"pages":[{"ns":6,"title":"File:Example upload.jpg"$fields}]}}"""

    @Test
    fun `unused file is tagged with G7 on its own page only`() {
        // Real response for an unused file: an empty "globalusage" and no "fileusage"
        enqueue(fixture("unused_file.json"))

        helper
            .requestDeletion(media, "I uploaded it less than 7 days ago and it is not used anywhere")
            .test()
            .assertValue(true)
            .assertComplete()

        val text = argumentCaptor<String>()
        val summary = argumentCaptor<String>()
        verify(pageEditClient).prependEdit(eq("File:Example upload.jpg"), text.capture(), summary.capture())
        assertEquals(
            "{{G7|reason=I uploaded it less than 7 days ago and it is not used anywhere}}\n",
            text.firstValue,
        )
        assertTrue(summary.firstValue.contains("G7"))
        // No deletion request page, log entry or talk page message
        verifyNoMoreInteractions(pageEditClient)

        val request = server.takeRequest()
        assertEquals("File:Example upload.jpg", request.requestUrl!!.queryParameter("titles"))
        assertEquals("globalusage|fileusage", request.requestUrl!!.queryParameter("prop"))
    }

    @Test
    fun `file used on wikis and on Commons is not tagged`() {
        enqueue(fixture("used_file.json"))

        helper
            .requestDeletion(media, "reason")
            .test()
            .assertError(FileInUseException::class.java)

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `file used only on another wiki is not tagged`() {
        enqueue(page(""","globalusage":[{"title":"Some article","wiki":"en.wikipedia.org"}]"""))

        helper.requestDeletion(media, "reason").test().assertError(FileInUseException::class.java)

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `file used only on Commons is not tagged`() {
        enqueue(page(""","globalusage":[],"fileusage":[{"ns":0,"title":"Gallery"}]"""))

        helper.requestDeletion(media, "reason").test().assertError(FileInUseException::class.java)

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `missing usage information is not treated as unused`() {
        // For example when the wiki does not report global usage
        enqueue(page(""))

        helper.requestDeletion(media, "reason").test().assertError(FileUsageUnknownException::class.java)

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `missing file is not tagged`() {
        enqueue(page(""","missing":true,"globalusage":[]"""))

        helper.requestDeletion(media, "reason").test().assertError(FileUsageUnknownException::class.java)

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `server error is not treated as unused`() {
        server.enqueue(MockResponse().setResponseCode(503))

        helper.requestDeletion(media, "reason").test().assertError { true }

        verify(pageEditClient, never()).prependEdit(any(), any(), any())
    }

    @Test
    fun `failed edit is reported`() {
        enqueue(fixture("unused_file.json"))
        whenever(pageEditClient.prependEdit(any(), any(), any()))
            .thenReturn(Observable.error(RuntimeException("Failed to prepend the page")))

        helper.requestDeletion(media, "reason").test().assertError(RuntimeException::class.java)
    }

    @Test
    fun `reason cannot break the template`() {
        enqueue(fixture("unused_file.json"))

        helper.requestDeletion(media, "a | b }} {{c").test().assertValue(true)

        val text = argumentCaptor<String>()
        verify(pageEditClient).prependEdit(any(), text.capture(), any())
        assertEquals("{{G7|reason=a   b  c}}\n", text.firstValue)
    }

    @Test
    fun `upload from a few days ago is within the time limit`() {
        val now = Date()
        val threeDaysAgo = Date(now.time - TimeUnit.DAYS.toMillis(3))

        assertTrue(SpeedyDeletionHelper.isWithinTimeLimit(threeDaysAgo, now))
    }

    @Test
    fun `upload from just under 7 days ago is within the time limit`() {
        val now = Date()
        val almostAWeek = Date(now.time - TimeUnit.DAYS.toMillis(7) + TimeUnit.MINUTES.toMillis(1))

        assertTrue(SpeedyDeletionHelper.isWithinTimeLimit(almostAWeek, now))
    }

    @Test
    fun `upload from 7 days ago or more is not within the time limit`() {
        val now = Date()

        assertFalse(SpeedyDeletionHelper.isWithinTimeLimit(Date(now.time - TimeUnit.DAYS.toMillis(7)), now))
        assertFalse(SpeedyDeletionHelper.isWithinTimeLimit(Date(now.time - TimeUnit.DAYS.toMillis(30)), now))
    }

    @Test
    fun `upload without a date or with a future date is not within the time limit`() {
        val now = Date()

        assertFalse(SpeedyDeletionHelper.isWithinTimeLimit(null, now))
        assertFalse(SpeedyDeletionHelper.isWithinTimeLimit(Date(now.time + TimeUnit.HOURS.toMillis(1)), now))
    }
}
