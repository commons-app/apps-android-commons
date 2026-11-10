package fr.free.nrw.commons

import okhttp3.logging.HttpLoggingInterceptor

/**
 * Hides credential-bearing headers in the interceptor's log output.
 */
fun HttpLoggingInterceptor.redactSensitiveHeaders(): HttpLoggingInterceptor =
    apply {
        redactHeader("Authorization")
        redactHeader("Cookie")
        redactHeader("Set-Cookie")
    }
