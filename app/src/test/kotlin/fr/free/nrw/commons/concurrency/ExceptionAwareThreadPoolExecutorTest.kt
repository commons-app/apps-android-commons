package fr.free.nrw.commons.concurrency

import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.FutureTask
import java.util.concurrent.ThreadFactory

class ExceptionAwareThreadPoolExecutorTest {
    private val reportedThrowables = mutableListOf<Throwable>()
    private val exceptionHandler =
        object : ExceptionHandler {
            override fun onException(t: Throwable) {
                reportedThrowables.add(t)
            }
        }
    private val executor =
        ExceptionAwareThreadPoolExecutor(
            1,
            ThreadFactory { runnable -> Thread(runnable) },
            exceptionHandler,
        )

    @Test
    fun reportsCauseOfFailedFuture() {
        val failure = IllegalStateException("failed")
        val task =
            FutureTask {
                throw failure
            }
        task.run()

        runAfterExecute(task, null)

        assertSame(failure, reportedThrowables.single())
    }

    @Test
    fun ignoresInterruptedFuture() {
        val task =
            object : FutureTask<Unit>({}) {
                override fun get(): Unit = throw InterruptedException("interrupted")
            }
        task.run()

        runAfterExecute(task, null)

        assertTrue(reportedThrowables.isEmpty())
    }

    @Test
    fun reportsThrowablePassedToAfterExecute() {
        val failure = AssertionError("failed")

        runAfterExecute(Runnable {}, failure)

        assertSame(failure, reportedThrowables.single())
    }

    private fun runAfterExecute(
        runnable: Runnable,
        throwable: Throwable?,
    ) {
        val method =
            ExceptionAwareThreadPoolExecutor::class.java.getDeclaredMethod(
                "afterExecute",
                Runnable::class.java,
                Throwable::class.java,
            )
        method.isAccessible = true
        method.invoke(executor, runnable, throwable)
    }
}
