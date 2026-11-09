package fr.free.nrw.commons

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InterceptorUtilsTest {
    private val server = MockWebServer()
    private val logLines = mutableListOf<String>()

    @Before
    fun setUp() {
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sensitiveHeadersShouldBeRedactedInBothDirections() {
        server.enqueue(
            MockResponse()
                .addHeader("Set-Cookie", "session=secret-cookie")
                .addHeader("Content-Type", "text/plain")
                .setBody("ok"),
        )
        val interceptor =
            HttpLoggingInterceptor { logLines.add(it) }
                .apply { level = HttpLoggingInterceptor.Level.HEADERS }
                .redactSensitiveHeaders()
        val client = OkHttpClient.Builder().addInterceptor(interceptor).build()

        client
            .newCall(
                Request
                    .Builder()
                    .url(server.url("/"))
                    .header("Authorization", "Bearer secret-token")
                    .header("Cookie", "session=secret-cookie")
                    .build(),
            ).execute()
            .close()

        listOf("Authorization", "Cookie", "Set-Cookie").forEach { name ->
            assertTrue("$name was not logged", logLines.any { it.startsWith("$name:") })
        }
        assertFalse(logLines.any { "secret" in it })
        assertTrue("Content-Type: text/plain" in logLines)
    }
}
