package fr.free.nrw.commons.concurrency

import fr.free.nrw.commons.BuildConfig


class BackgroundPoolExceptionHandler : ExceptionHandler {
    /**
     * If an exception occurs on a background thread, this handler will crash for debug builds
     * but fail silently for release builds.
     * @param t
     */
    override fun onException(t: Throwable) {
        // Crash for debug build
        if (BuildConfig.DEBUG) {
            val thread = Thread {
                throw RuntimeException(t)
            }
            thread.start()
        }
    }
}
