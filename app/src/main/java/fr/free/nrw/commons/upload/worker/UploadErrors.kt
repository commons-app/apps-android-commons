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

/**
 * Server error codes that retrying the same file with the same name can never fix.
 * Most of them come from UploadBase::convertVerifyErrorToStatus() in MediaWiki.
 * verification-error is left out, because a chunk damaged during the upload can cause it,
 * and uploading the file again can then succeed.
 */
private val permanentUploadErrors =
    setOf(
        "filename-tooshort",
        "illegal-filename",
        "filename-toolong",
        "filetype-missing",
        "windows-nonascii-filename",
        "empty-file",
        "file-too-large",
        "filetype-banned",
        // From the TitleBlacklist extension on Commons, for example a caption that is only "§".
        "titleblacklist-forbidden",
    )

/**
 * Returns true if the upload failed for a reason that retrying cannot fix.
 */
internal fun isPermanentUploadError(errorInfo: String?): Boolean = errorInfo in permanentUploadErrors

private const val HTTP_TOO_MANY_REQUESTS = 429
private const val HTTP_SERVER_ERROR_FIRST = 500
private const val HTTP_SERVER_ERROR_LAST = 599

// "HTTP" and a status code, as in "Unsuccessful response: HTTP 503" from UnsuccessfulResponseInterceptor,
// or "HTTP 503 " from Retrofit for the publish request, which that interceptor skips
private val httpStatusInError = Regex("""\bHTTP (\d{3})\b""")

/**
 * Returns the HTTP status code in the error of a failed upload, or null if there is none.
 */
internal fun httpStatusCode(errorInfo: String?): Int? =
    errorInfo
        ?.let { httpStatusInError.find(it) }
        ?.groupValues
        ?.get(1)
        ?.toInt()

/**
 * Returns true for server problems (5xx) and too many requests (429),
 * where trying again later usually works.
 */
internal fun isTemporaryServerError(httpStatusCode: Int): Boolean =
    httpStatusCode == HTTP_TOO_MANY_REQUESTS ||
        httpStatusCode in HTTP_SERVER_ERROR_FIRST..HTTP_SERVER_ERROR_LAST
