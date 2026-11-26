package fr.free.nrw.commons.upload.worker

import android.content.Context
import fr.free.nrw.commons.R
import fr.free.nrw.commons.UnsuccessfulResponseException
import fr.free.nrw.commons.retryAfterMillis
import retrofit2.HttpException
import java.io.IOException
import java.util.Date
import java.util.concurrent.TimeUnit

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

// MediaWiki's own limit on uploads per minute. It comes back as an API error, not as HTTP 429
private const val RATE_LIMITED = "ratelimited"
private const val FIRST_RETRY_WAIT_MILLIS = 5_000L

// Also limits Retry-After, so that a wrong header can't stop automatic retries for days
private val MAX_RETRY_WAIT_MILLIS = TimeUnit.HOURS.toMillis(1)
private const val MAX_WAIT_DOUBLINGS = 20

/**
 * Returns how long the server asked to wait before trying again, from the Retry-After header of a
 * failed request, or null if it didn't say.
 */
internal fun Throwable.retryAfterMillis(): Long? =
    when (this) {
        is UnsuccessfulResponseException -> retryAfterMillis
        // The publish request skips UnsuccessfulResponseInterceptor, so Retrofit reports its errors
        is HttpException -> response()?.headers()?.retryAfterMillis()
        else -> cause?.retryAfterMillis()
    }

/**
 * Returns the time before which a failed upload should not be retried automatically, or null if
 * it can be retried right away.
 *
 * Only too many requests, server problems and MediaWiki's rate limit wait. The wait is the one
 * the server asked for, otherwise 5 seconds doubled for every retry already made.
 *
 * @param retries how many times the upload was already retried automatically
 */
internal fun nextAutomaticRetry(
    errorInfo: String?,
    retryAfterMillis: Long?,
    retries: Int,
    nowMillis: Long,
): Date? {
    val code = httpStatusCode(errorInfo)
    val temporary = errorInfo == RATE_LIMITED || (code != null && isTemporaryServerError(code))
    if (!temporary) {
        return null
    }
    val wait = retryAfterMillis ?: (FIRST_RETRY_WAIT_MILLIS shl retries.coerceIn(0, MAX_WAIT_DOUBLINGS))
    return Date(nowMillis + wait.coerceIn(0, MAX_RETRY_WAIT_MILLIS))
}

/**
 * Returns the reason to show for a failed upload: a message the user can understand for server
 * problems and too many requests, or the saved error as it is for everything else.
 */
internal fun uploadErrorMessage(
    context: Context,
    errorInfo: String?,
): String? {
    val code = httpStatusCode(errorInfo)
    return if (code != null && isTemporaryServerError(code)) {
        context.getString(R.string.upload_failed_server_error, code)
    } else {
        errorInfo
    }
}
