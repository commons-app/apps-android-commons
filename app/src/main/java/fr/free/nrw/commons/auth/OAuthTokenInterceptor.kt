package fr.free.nrw.commons.auth

import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.CommonsApplication
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class OAuthTokenInterceptor(
    private val sessionManagerProvider: () -> SessionManager? = {
        CommonsApplication.instance?.sessionManager
    },
) : Interceptor {
    constructor(sessionManager: SessionManager) : this({ sessionManager })

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()
        val accessToken = sessionManagerProvider()?.getAccessToken()

        if (shouldAttachToken(request, accessToken)) {
            requestBuilder.header("Authorization", "Bearer $accessToken")
        }

        return chain.proceed(requestBuilder.build())
    }

    private fun shouldAttachToken(
        request: Request,
        accessToken: String?,
    ): Boolean {
        if (accessToken.isNullOrEmpty() || request.header("Authorization") != null) {
            return false
        }
        val allowedHosts =
            setOfNotNull(
                BuildConfig.COMMONS_URL.toHttpUrlOrNull()?.host,
                BuildConfig.WIKIDATA_URL.toHttpUrlOrNull()?.host,
                "meta.wikimedia.org",
                "meta.wikimedia.beta.wmflabs.org",
            )
        return request.url.isHttps && request.url.host in allowedHosts
    }
}
