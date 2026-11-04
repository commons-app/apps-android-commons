package fr.free.nrw.commons.upload.worker

import io.reactivex.Observable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
}
