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
                            }
                            // If error is null, just log it - don't throw
                            if (mwError == null) {
                                Timber.w("Malformed MediaWiki error response: error field is null")
                            }
                        }
                    }
                }
            } catch (e: IOException) {
                // Only catch IOException/MwIOException - NPE is prevented by null checks
                if (suppressErrors && e is MwIOException) {
                    Timber.d(e, "Suppressed (known) error")
                } else {
                    throw e
                }
            }
            return rsp
        }
        throw IOException("Unsuccessful response")
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