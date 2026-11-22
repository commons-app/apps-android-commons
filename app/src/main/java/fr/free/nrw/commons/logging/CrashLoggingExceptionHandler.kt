package fr.free.nrw.commons.logging

/**
 * Writes the crash to the log file, then passes it to the previous handler (ACRA).
 */
class CrashLoggingExceptionHandler(
    private val logCrash: (Thread, Throwable) -> Unit,
    private val previousHandler: Thread.UncaughtExceptionHandler?,
) : Thread.UncaughtExceptionHandler {
    override fun uncaughtException(
        thread: Thread,
        throwable: Throwable,
    ) {
        try {
            logCrash(thread, throwable)
        } finally {
            // Pass the crash on even if writing it to the log failed
            previousHandler?.uncaughtException(thread, throwable)
        }
    }
}
