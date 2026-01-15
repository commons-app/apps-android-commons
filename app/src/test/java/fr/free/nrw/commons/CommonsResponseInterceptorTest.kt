package fr.free.nrw.commons

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class CommonsResponseInterceptorTest {

    private val interceptor = CommonsResponseInterceptor()

    @Test
    fun testInterceptDoesNotCrashOnMalformedErrorJson() {
        val chain = mock(Interceptor.Chain::class.java)
        val request = Request.Builder()
            .url("https://commons.wikimedia.org/w/api.php")
            .build()

        val badJson = "{\"error\": null}"
        val responseBody = badJson.toResponseBody("application/json".toMediaTypeOrNull())

        val response = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(200)
            .message("OK")
            .body(responseBody)
            .build()

        `when`(chain.request()).thenReturn(request)

        // FIX: Use 'any() ?: request'
        // 'any()' registers the matcher (so it accepts the modified request).
        // '?: request' provides a fallback value so Kotlin doesn't crash on the null return.
        `when`(chain.proceed(ArgumentMatchers.any() ?: request)).thenReturn(response)

        val result = interceptor.intercept(chain)

        assertNotNull(result)
        assertNotNull(result.body)
    }
}