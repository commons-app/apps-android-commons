package fr.free.nrw.commons.upload.worker

import java.io.IOException

/**
 * Returns the error code of a failed upload,
 * or the exception message if there is no code.
 *
 * UploadClient throws an IOException containing the server error code,
 * which may be wrapped in a RuntimeException by blockingSingle().
 */
internal fun Throwable.uploadErrorCode(): String? {
    val cause = cause
    return if (this !is IOException && cause is IOException) cause.message else message
}
