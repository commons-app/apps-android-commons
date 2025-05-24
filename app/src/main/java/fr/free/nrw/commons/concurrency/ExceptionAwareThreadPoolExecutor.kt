package fr.free.nrw.commons.concurrency

import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Future
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory


class ExceptionAwareThreadPoolExecutor(
    corePoolSize: Int,
    threadFactory: ThreadFactory,
    private val exceptionHandler: ExceptionHandler?
) : ScheduledThreadPoolExecutor(corePoolSize, threadFactory) {

    override fun afterExecute(r: Runnable, t: Throwable?) {
        super.afterExecute(r, t)
        var throwable = t

        if (throwable == null && r is Future<*>) {
            try {
                if (r.isDone) {
                    r.get()
                }
            } catch (_: CancellationException) {
                // ignore
            } catch (_: InterruptedException) {
                // ignore
            } catch (e: ExecutionException) {
                throwable = e.cause ?: e
            } catch (e: Exception) {
                throwable = e
            }
        }

        throwable?.let {
            exceptionHandler?.onException(it)
        }
    }
}