package fr.free.nrw.commons.upload.worker

import android.content.Context
import fr.free.nrw.commons.R
import fr.free.nrw.commons.UnsuccessfulResponseException
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException
import java.util.Date

class UploadErrorsTest {
    @Test
    fun `uploadErrorCode unwraps the IOException that blockingSingle wraps`() {
        val exception = RuntimeException(IOException("filetype-banned"))

        assertEquals("filetype-banned", exception.uploadErrorCode())
    }

    @Test
    fun `uploadErrorCode returns the server code thrown by blockingSingle`() {
        val exception =
            assertThrows(RuntimeException::class.java) {
                Observable.error<Any>(IOException("verification-error")).blockingSingle()
            }

        assertEquals("verification-error", exception.uploadErrorCode())
    }

    @Test
    fun `uploadErrorCode returns the code of a plain IOException`() {
        val exception = IOException("stashfailed")

        assertEquals("stashfailed", exception.uploadErrorCode())
    }

    @Test
    fun `uploadErrorCode returns the message of other exceptions`() {
        val exception = IllegalStateException("Something else went wrong")

        assertEquals("Something else went wrong", exception.uploadErrorCode())
    }

    @Test
    fun `isPermanentUploadError is true for errors that a retry cannot fix`() {
        assertTrue(isPermanentUploadError("filetype-banned"))
        assertTrue(isPermanentUploadError("illegal-filename"))
        assertTrue(isPermanentUploadError("file-too-large"))
        assertTrue(isPermanentUploadError("titleblacklist-forbidden"))
    }

    @Test
    fun `isPermanentUploadError is false for errors that a retry can fix`() {
        assertFalse(isPermanentUploadError("verification-error"))
        assertFalse(isPermanentUploadError("stashfailed"))
        assertFalse(isPermanentUploadError("timeout"))
    }

    @Test
    fun `isPermanentUploadError is false when the reason is unknown`() {
        assertFalse(isPermanentUploadError(null))
    }

    @Test
    fun `httpStatusCode reads the code from the interceptor message`() {
        assertEquals(503, httpStatusCode("Unsuccessful response: HTTP 503"))
    }

    @Test
    fun `httpStatusCode reads the code from the Retrofit message of the publish request`() {
        assertEquals(503, httpStatusCode("HTTP 503 "))
    }

    @Test
    fun `httpStatusCode is null when the error has no HTTP status code`() {
        assertNull(httpStatusCode("filetype-banned"))
        assertNull(httpStatusCode("badtoken"))
        assertNull(httpStatusCode("Unsuccessful response"))
        assertNull(httpStatusCode(null))
    }

    @Test
    fun `isTemporaryServerError is true for server errors and too many requests`() {
        assertTrue(isTemporaryServerError(500))
        assertTrue(isTemporaryServerError(503))
        assertTrue(isTemporaryServerError(599))
        assertTrue(isTemporaryServerError(429))
    }

    @Test
    fun `isTemporaryServerError is false for other codes`() {
        assertFalse(isTemporaryServerError(403))
        assertFalse(isTemporaryServerError(413))
        assertFalse(isTemporaryServerError(499))
        assertFalse(isTemporaryServerError(600))
    }

    @Test
    fun `uploadErrorMessage shows the friendly message for a temporary server error`() {
        val context = mockk<Context>()
        every { context.getString(R.string.upload_failed_server_error, 503) } returns FRIENDLY_503

        assertEquals(FRIENDLY_503, uploadErrorMessage(context, "Unsuccessful response: HTTP 503"))
        assertEquals(FRIENDLY_503, uploadErrorMessage(context, "HTTP 503 "))
    }

    @Test
    fun `uploadErrorMessage keeps other errors as they are`() {
        // Any call on this context would fail the test, so no friendly message is looked up
        val context = mockk<Context>()

        assertEquals("Unsuccessful response: HTTP 403", uploadErrorMessage(context, "Unsuccessful response: HTTP 403"))
        assertEquals("filetype-banned", uploadErrorMessage(context, "filetype-banned"))
        assertNull(uploadErrorMessage(context, null))
    }

    @Test
    fun `nextAutomaticRetry waits as long as Retry-After asks`() {
        assertEquals(Date(NOW + 120_000), nextAutomaticRetry("Unsuccessful response: HTTP 503", 120_000, 0, NOW))
        assertEquals(Date(NOW + 30_000), nextAutomaticRetry("HTTP 429 ", 30_000, 3, NOW))
    }

    @Test
    fun `nextAutomaticRetry doubles a 5 second wait for every retry without Retry-After`() {
        assertEquals(Date(NOW + 5_000), nextAutomaticRetry("Unsuccessful response: HTTP 503", null, 0, NOW))
        assertEquals(Date(NOW + 10_000), nextAutomaticRetry("Unsuccessful response: HTTP 503", null, 1, NOW))
        assertEquals(Date(NOW + 2_560_000), nextAutomaticRetry("Unsuccessful response: HTTP 503", null, 9, NOW))
    }

    @Test
    fun `nextAutomaticRetry waits for MediaWiki's rate limit too`() {
        assertEquals(Date(NOW + 5_000), nextAutomaticRetry("ratelimited", null, 0, NOW))
    }

    @Test
    fun `nextAutomaticRetry waits at most one hour`() {
        val oneHour = 3_600_000L
        assertEquals(Date(NOW + oneHour), nextAutomaticRetry("Unsuccessful response: HTTP 503", 86_400_000, 0, NOW))
        assertEquals(Date(NOW + oneHour), nextAutomaticRetry("Unsuccessful response: HTTP 503", null, 50, NOW))
    }

    @Test
    fun `nextAutomaticRetry does not wait for other errors`() {
        assertNull(nextAutomaticRetry("Unsuccessful response: HTTP 403", 120_000, 0, NOW))
        assertNull(nextAutomaticRetry("stashfailed", null, 0, NOW))
        assertNull(nextAutomaticRetry(null, null, 0, NOW))
    }

    @Test
    fun `retryAfterMillis comes from the interceptor exception, also when wrapped`() {
        val exception = UnsuccessfulResponseException(503, 120_000)

        assertEquals(120_000L, exception.retryAfterMillis())
        assertEquals(120_000L, RuntimeException(exception).retryAfterMillis())
        assertNull(IOException("stashfailed").retryAfterMillis())
    }

    @Test
    fun `retryAfterMillis comes from the Retrofit error of the publish request`() {
        val response =
            Response.error<Any>(
                "".toResponseBody(),
                okhttp3.Response
                    .Builder()
                    .request(Request.Builder().url("https://commons.wikimedia.org/w/api.php").build())
                    .protocol(Protocol.HTTP_1_1)
                    .code(429)
                    .message("Too Many Requests")
                    .header("Retry-After", "30")
                    .build(),
            )

        assertEquals(30_000L, HttpException(response).retryAfterMillis())
    }

    private companion object {
        const val NOW = 1_000_000_000_000L
        const val FRIENDLY_503 = "Commons could not handle the request right now. Please try again later. (HTTP 503)"
    }
}
