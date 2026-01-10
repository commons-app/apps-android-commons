package fr.free.nrw.commons

import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.wikidata.GsonUtil
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import fr.free.nrw.commons.wikidata.mwapi.MwErrorResponse
import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwLegacyServiceError
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

object OkHttpConnectionFactory {
    private const val CACHE_DIR_NAME = "okhttp-cache"
    private const val NET_CACHE_SIZE = (64 * 1024 * 1024).toLong()

    @VisibleForTesting
    var CLIENT: OkHttpClient? = null

    fun getClient(cookieJar: CommonsCookieJar): OkHttpClient {
        if (CLIENT == null) {
            CLIENT = createClient(cookieJar)
        }
        return CLIENT!!
    }

    private fun createClient(cookieJar: CommonsCookieJar): OkHttpClient {
        return OkHttpClient.Builder()
            .cookieJar(cookieJar)
            .cache(
                if (CommonsApplication.instance != null) Cache(
                    File(CommonsApplication.instance.cacheDir, CACHE_DIR_NAME),
                    NET_CACHE_SIZE
                ) else null
            )
            .connectTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .addInterceptor(HttpLoggingInterceptor().apply {
                setLevel(HttpLoggingInterceptor.Level.BASIC)
                redactHeader("Authorization")
                redactHeader("Cookie")
            })
            .addInterceptor(UnsuccessfulResponseInterceptor())
            .addInterceptor(CommonHeaderRequestInterceptor())
            .build()
    }
}

class CommonHeaderRequestInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request().newBuilder()
            .header("User-Agent", CommonsApplication.instance.userAgent)
            .build()
        return chain.proceed(request)
    }
}

private const val SUPPRESS_ERROR_LOG = "x-commons-suppress-error-log"
const val SUPPRESS_ERROR_LOG_HEADER: String = "$SUPPRESS_ERROR_LOG: true"

private class UnsuccessfulResponseInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val rq = chain.request()

        // If the request contains our special "suppress errors" header, make note of it
        // but don't pass that on to the server.
        val suppressErrors = rq.headers.names().contains(SUPPRESS_ERROR_LOG)
        val request = rq.newBuilder()
            .removeHeader(SUPPRESS_ERROR_LOG)
            .build()

        val rsp = chain.proceed(request)

        // Do not intercept certain requests and let the caller handle the errors
        if (isExcludedUrl(chain.request())) {
            return rsp
        }
        if (rsp.isSuccessful) {
            try {
                rsp.peekBody(ERRORS_PREFIX.length.toLong()).use { responseBody ->
                    if (ERRORS_PREFIX == responseBody.string()) {
                        rsp.body.use { body ->
                            val bodyString = body!!.string()

                            throw MwIOException(
                                "MediaWiki API returned error: $bodyString",
                                GsonUtil.defaultGson.fromJson(
                                    bodyString,
                                    MwErrorResponse::class.java
                                ).error!!,
                            )
                        }
                    }
                }
            } catch (e: MwIOException) {
                // Log the error as debug (and therefore, "expected") or at error level
                if (suppressErrors) {
                    Timber.d(e, "Suppressed (known / expected) error")
                } else {
                    Timber.e(e)
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
