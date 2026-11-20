package fr.free.nrw.commons.logging

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test

class CrashLoggingExceptionHandlerTest {
    private val thread = Thread("main")
    private val crash = IllegalStateException("Something went wrong")

    @Test
    fun `the crash is written to the log before it is passed on`() {
        val steps = mutableListOf<String>()
        val handler =
            CrashLoggingExceptionHandler(
                logCrash = { _, _ -> steps.add("log") },
                previousHandler = { _, _ -> steps.add("previous handler") },
            )

        handler.uncaughtException(thread, crash)

        assertEquals(listOf("log", "previous handler"), steps)
    }

    @Test
    fun `the log and the previous handler get the same thread and crash`() {
        var loggedThread: Thread? = null
        var loggedCrash: Throwable? = null
        var passedThread: Thread? = null
        var passedCrash: Throwable? = null
        val handler =
            CrashLoggingExceptionHandler(
                logCrash = { crashedThread, throwable ->
                    loggedThread = crashedThread
                    loggedCrash = throwable
                },
                previousHandler = { crashedThread, throwable ->
                    passedThread = crashedThread
                    passedCrash = throwable
                },
            )

        handler.uncaughtException(thread, crash)

        assertSame(thread, loggedThread)
        assertSame(crash, loggedCrash)
        assertSame(thread, passedThread)
        assertSame(crash, passedCrash)
    }

    @Test
    fun `the crash is passed on even if writing it to the log fails`() {
        var passedCrash: Throwable? = null
        val handler =
            CrashLoggingExceptionHandler(
                logCrash = { _, _ -> throw IllegalStateException("The log file cannot be written") },
                previousHandler = { _, throwable -> passedCrash = throwable },
            )

        assertThrows(IllegalStateException::class.java) {
            handler.uncaughtException(thread, crash)
        }

        assertSame(crash, passedCrash)
    }

    @Test
    fun `the crash is still logged when there is no previous handler`() {
        var loggedCrash: Throwable? = null
        val handler =
            CrashLoggingExceptionHandler(
                logCrash = { _, throwable -> loggedCrash = throwable },
                previousHandler = null,
            )

        handler.uncaughtException(thread, crash)

        assertSame(crash, loggedCrash)
    }
}
