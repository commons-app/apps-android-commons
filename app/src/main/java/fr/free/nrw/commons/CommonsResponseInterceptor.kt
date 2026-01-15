package fr.free.nrw.commons

import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.mwapi.MwErrorResponse
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import timber.log.Timber
import java.io.IOException

private const val SUPPRESS_ERROR_LOG = "x-commons-suppress-error-log"
const val SUPPRESS_ERROR_LOG_HEADER: String = "$SUPPRESS_ERROR_LOG: true"

class CommonsResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        try {
            val rq = chain.request()
            val suppressErrors = rq.headers.names().contains(SUPPRESS_ERROR_LOG)
            val request = rq.newBuilder()
                .removeHeader(SUPPRESS_ERROR_LOG)
                .build()

            val rsp = chain.proceed(request)

            if (isExcludedUrl(request)) {
                return rsp
            }

            if (rsp.isSuccessful) {
                // Use a dedicated try-catch for the parsing logic to catch ANY crash (NPE, etc.)
                try {
                    rsp.peekBody(ERRORS_PREFIX.length.toLong()).use { responseBody ->
                        if (ERRORS_PREFIX == responseBody.string()) {
                            rsp.body?.use { body ->
                                val bodyString = body.string()
                                val errorResponse = GsonUtil.defaultGson.fromJson(
                                    bodyString,
                                    MwErrorResponse::class.java
                                )
                                val mwError = errorResponse?.error
                                if (mwError != null) {
                                    throw MwIOException(
                                        "MediaWiki API returned error: $bodyString",
                                        mwError
                                    )
                                } else {
                                    // Body consumed but error is null = malformed response
                                    throw IOException("Malformed MediaWiki error response: error field is null")
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    // If it's already an IO/MwIOException, let it through.
                    // If it's a RuntimeException (like NPE), wrap it in IOException so we don't crash.
                    if (e is IOException) {
                        if (suppressErrors && e is MwIOException) {
                            Timber.d(e, "Suppressed (known) error")
                        } else {
                            throw e
                        }
                    } else {
                        // This catches NullPointerException and turns it into a safe failure
                        throw IOException("Safe failure: Error parsing response", e)
                    }
                }
                return rsp
            }
            throw IOException("Unsuccessful response")
        } catch (t: RuntimeException) {
            // "Nuclear Shield": If ANYTHING above threw a RuntimeException (NPE),
            // catch it here and convert to IOException.
            throw IOException("Interceptor crashed with ${t.javaClass.simpleName}", t)
        }
    }

    private fun isExcludedUrl(request: Request): Boolean {
        val requestUrl = request.url.toString()
        for (url in DO_NOT_INTERCEPT) {
            if (requestUrl.contains(url)) {
                return true
            }
        }
        return false
    }

    companion object {
        val DO_NOT_INTERCEPT = listOf(
            "api.php?format=json&formatversion=2&errorformat=plaintext&action=upload&ignorewarnings=1"
        )
        const val ERRORS_PREFIX = "{\"error"
    }
}