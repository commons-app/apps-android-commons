package fr.free.nrw.commons

import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.io.IOException

class UnsuccessfulResponseInterceptorTest {
    private interface TestApi {
        @GET("w/api.php?format=json&formatversion=2&errorformat=plaintext&action=query")
        fun query(): Call<MwQueryResponse>
    }

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client =
            OkHttpClient
                .Builder()
                .addInterceptor(UnsuccessfulResponseInterceptor())
                .build()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun plaintextErrorsResponseCanStillBeRead() {
        // errorformat=plaintext returns "errors", which starts with the same prefix as "error"
        server.enqueue(MockResponse().setBody(PLAINTEXT_ERRORS))

        val response = client.newCall(request()).execute()

        assertEquals(PLAINTEXT_ERRORS, response.body!!.string())
    }

    @Test
    fun plaintextErrorsReachTheCallerAsMwException() {
        server.enqueue(MockResponse().setBody(PLAINTEXT_ERRORS))
        val api =
            Retrofit
                .Builder()
                .baseUrl(server.url("/"))
                .client(client)
                .addConverterFactory(GsonConverterFactory.create(GsonUtil.defaultGson))
                .build()
                .create(TestApi::class.java)

        val exception = assertThrows(MwException::class.java) { api.query().execute() }

        assertEquals("illegal-filename", exception.errorCode)
    }

    @Test
    fun legacyErrorStillThrowsMwIOException() {
        server.enqueue(MockResponse().setBody(LEGACY_ERROR))

        val exception =
            assertThrows(MwIOException::class.java) {
                client.newCall(request()).execute()
            }

        assertEquals("badtoken", exception.error.code)
    }

    @Test
    fun successfulResponseIsNotChanged() {
        server.enqueue(MockResponse().setBody(SUCCESS))

        val response = client.newCall(request()).execute()

        assertEquals(SUCCESS, response.body!!.string())
    }

    @Test
    fun httpErrorStatusIsInTheExceptionMessage() {
        server.enqueue(MockResponse().setResponseCode(503))

        val exception =
            assertThrows(IOException::class.java) {
                client.newCall(request()).execute()
            }

        assertEquals("Unsuccessful response: HTTP 503", exception.message)
    }

    @Test
    fun httpErrorKeepsRetryAfterInSeconds() {
        server.enqueue(MockResponse().setResponseCode(503).setHeader("Retry-After", "120"))

        val exception =
            assertThrows(UnsuccessfulResponseException::class.java) {
                client.newCall(request()).execute()
            }

        assertEquals("Unsuccessful response: HTTP 503", exception.message)
        assertEquals(120_000L, exception.retryAfterMillis)
    }

    @Test
    fun httpErrorWithoutRetryAfterHasNoWait() {
        server.enqueue(MockResponse().setResponseCode(429))

        val exception =
            assertThrows(UnsuccessfulResponseException::class.java) {
                client.newCall(request()).execute()
            }

        assertNull(exception.retryAfterMillis)
    }

    @Test
    fun retryAfterCanBeAnHttpDate() {
        val headers = Headers.headersOf("Retry-After", "Wed, 21 Oct 2015 07:28:00 GMT")
        val twoMinutesBefore = 1_445_412_480_000L - 120_000L

        assertEquals(120_000L, headers.retryAfterMillis(twoMinutesBefore))
    }

    @Test
    fun retryAfterInThePastOrUnreadableIsNoWait() {
        val past = Headers.headersOf("Retry-After", "Wed, 21 Oct 2015 07:28:00 GMT")

        assertEquals(0L, past.retryAfterMillis(2_000_000_000_000L))
        assertNull(Headers.headersOf("Retry-After", "soon").retryAfterMillis())
    }

    @Test
    fun connectionIsReusedAfterHttpError() {
        // With an empty body the connection is released anyway, so the error page needs a body
        server.enqueue(MockResponse().setResponseCode(503).setBody("Service Unavailable"))
        server.enqueue(MockResponse().setBody(SUCCESS))

        assertThrows(IOException::class.java) { client.newCall(request()).execute() }
        client.newCall(request()).execute().close()

        server.takeRequest()
        // 0 would mean that a new connection was opened, because the first one was never released
        assertEquals(1, server.takeRequest().sequenceNumber)
    }

    private fun request() = Request.Builder().url(server.url("/w/api.php")).build()

    private companion object {
        const val PLAINTEXT_ERRORS =
            """{"errors":[{"code":"illegal-filename","text":"The filename is not allowed.","module":"upload"}]}"""
        const val LEGACY_ERROR = """{"error":{"code":"badtoken","info":"Invalid CSRF token."}}"""
        const val SUCCESS = """{"batchcomplete":true,"query":{"pages":[]}}"""
    }
}
