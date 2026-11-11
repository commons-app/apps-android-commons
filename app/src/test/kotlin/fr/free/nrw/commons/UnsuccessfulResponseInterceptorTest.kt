package fr.free.nrw.commons

import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwException
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwQueryResponse
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET

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

    private fun request() = Request.Builder().url(server.url("/w/api.php")).build()

    private companion object {
        const val PLAINTEXT_ERRORS =
            """{"errors":[{"code":"illegal-filename","text":"The filename is not allowed.","module":"upload"}]}"""
        const val LEGACY_ERROR = """{"error":{"code":"badtoken","info":"Invalid CSRF token."}}"""
        const val SUCCESS = """{"batchcomplete":true,"query":{"pages":[]}}"""
    }
}
