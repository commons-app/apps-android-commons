package fr.free.nrw.commons

import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.fail
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.io.IOException

class CommonsResponseInterceptorTest {

    @Test
    fun testInterceptDoesNotCrashOnMalformedErrorJson() {
        // 1. Mock the OkHttp Chain and Request
        val chain = mock(Interceptor.Chain::class.java)
        val request = Request.Builder()
            .url("https://commons.wikimedia.org/w/api.php")
            .build()

        // 2. Create a "Bad" Response
        // This body simulates the server returning an incomplete or null error
        // which previously caused the NullPointerException
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
        `when`(chain.proceed(request)).thenReturn(response)

        // 3. Create the interceptor
        val interceptor = CommonsResponseInterceptor()

        // 4. Run the test
        try {
            interceptor.intercept(chain)
            // If the code reaches here, it might have just logged the error without crashing, which is success.
        } catch (e: NullPointerException) {
            // FAILURE: This means the app crashed
            fail("The app crashed with NullPointerException! The fix is not working.")
        } catch (e: Exception) {
            // SUCCESS: It threw a handled exception (like IOException) instead of crashing
            println("Test Passed: Caught expected exception: ${e.javaClass.simpleName}")
        }
    }
}