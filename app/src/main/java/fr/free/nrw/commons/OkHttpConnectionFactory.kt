package fr.free.nrw.commons

import androidx.annotation.VisibleForTesting
import fr.free.nrw.commons.wikidata.cookies.CommonsCookieJar
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
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
            .addInterceptor(CommonsResponseInterceptor())
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