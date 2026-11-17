package fr.free.nrw.commons.upload.worker

import android.content.Context
import fr.free.nrw.commons.R
import io.mockk.every
import io.mockk.mockk
import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

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

    private companion object {
        const val FRIENDLY_503 = "Commons could not handle the request right now. Please try again later. (HTTP 503)"
    }
}
