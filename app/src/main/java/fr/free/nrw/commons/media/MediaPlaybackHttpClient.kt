package fr.free.nrw.commons.media

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

/**
 * Builds the client used for media playback.
 *
 * Derived from the shared client so headers such as UserAgent are kept, but
 * without the shared response cache, which does not suit large media streams,
 * and with its logging interceptor swapped for 'playbackLoggingInterceptor',
 * which logs at most headers so media bodies are never read for logging.
 */
fun mediaPlaybackHttpClient(
    sharedClient: OkHttpClient,
    playbackLoggingInterceptor: HttpLoggingInterceptor,
): OkHttpClient =
    sharedClient
        .newBuilder()
        .cache(null)
        .apply {
            interceptors().removeAll { it is HttpLoggingInterceptor }
            interceptors().add(0, playbackLoggingInterceptor)
        }.build()
