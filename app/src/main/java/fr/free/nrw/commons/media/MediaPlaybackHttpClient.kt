package fr.free.nrw.commons.media

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Builds the client used for media playback.
 *
 * Derived from the shared client so headers such as UserAgent are kept, but
 * without body logging or the shared response cache, neither of which suits
 * large media streams.
 */
fun mediaPlaybackHttpClient(
    sharedClient: OkHttpClient,
    loggingInterceptor: HttpLoggingInterceptor,
): OkHttpClient =
    sharedClient.newBuilder()
        .cache(null)
        .apply { interceptors().remove(loggingInterceptor) }
        .build()
