package fr.free.nrw.commons.auth

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.concurrent.TimeUnit

class OAuthTokenInterceptorTest {
    private val sessionManager: SessionManager = mock()
    private val interceptor = OAuthTokenInterceptor(sessionManager)

    @Test
    fun attachesBearerTokenForAllowedWikimediaHostOverHttps() {
        whenever(sessionManager.getAccessToken()).thenReturn("test_token_123")
        val request =
            Request
                .Builder()
                .url("https://meta.wikimedia.org/w/api.php")
                .build()

        var capturedAuthHeader: String? = null
        interceptor.intercept(createFakeChain(request) { capturedAuthHeader = it.header("Authorization") })

        assertEquals("Bearer test_token_123", capturedAuthHeader)
    }

    @Test
    fun doesNotAttachTokenForDisallowedHost() {
        whenever(sessionManager.getAccessToken()).thenReturn("test_token_123")
        val request =
            Request
                .Builder()
                .url("https://evil.example.com/api")
                .build()

        var capturedAuthHeader: String? = null
        interceptor.intercept(createFakeChain(request) { capturedAuthHeader = it.header("Authorization") })

        assertNull(capturedAuthHeader)
    }

    @Test
    fun doesNotAttachTokenOverHttp() {
        whenever(sessionManager.getAccessToken()).thenReturn("test_token_123")
        val request =
            Request
                .Builder()
                .url("http://meta.wikimedia.org/w/api.php")
                .build()

        var capturedAuthHeader: String? = null
        interceptor.intercept(createFakeChain(request) { capturedAuthHeader = it.header("Authorization") })

        assertNull(capturedAuthHeader)
    }

    @Test
    fun doesNotOverwriteExistingAuthorizationHeader() {
        whenever(sessionManager.getAccessToken()).thenReturn("new_token")
        val request =
            Request
                .Builder()
                .url("https://meta.wikimedia.org/w/api.php")
                .header("Authorization", "Bearer existing_token")
                .build()

        var capturedAuthHeader: String? = null
        interceptor.intercept(createFakeChain(request) { capturedAuthHeader = it.header("Authorization") })

        assertEquals("Bearer existing_token", capturedAuthHeader)
    }

    private fun createFakeChain(
        request: Request,
        onProceed: (Request) -> Unit,
    ): Interceptor.Chain =
        object : Interceptor.Chain {
            override fun request(): Request = request

            override fun proceed(request: Request): Response {
                onProceed(request)
                return Response
                    .Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .build()
            }

            override fun connection() = null

            override fun call() = throw UnsupportedOperationException()

            override fun connectTimeoutMillis() = 1000

            override fun withConnectTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun readTimeoutMillis() = 1000

            override fun withReadTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this

            override fun writeTimeoutMillis() = 1000

            override fun withWriteTimeout(
                timeout: Int,
                unit: TimeUnit,
            ) = this
        }
}
