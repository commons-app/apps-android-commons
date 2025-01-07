package fr.free.nrw.commons.concurrency

import java.util.concurrent.Callable
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit


/**
 * This class is a thread pool which provides some additional features:
 * - it sets the thread priority to a value lower than foreground priority by default, or you can
 * supply your own priority
 * - it gives you a way to handle exceptions thrown in the thread pool
 */
class ThreadPoolService private constructor(builder: Builder) : Executor {
    private val backgroundPool: ScheduledThreadPoolExecutor = ExceptionAwareThreadPoolExecutor(
        builder.poolSize,
        object : ThreadFactory {
            private var count = 0
            override fun newThread(r: Runnable): Thread {
                count++
                val t = Thread(r, "${builder.name}-$count")
                // If the priority is specified out of range, we set the thread priority to
                // Thread.MIN_PRIORITY
                // It's done to prevent IllegalArgumentException and to prevent setting of
                // improper high priority for a less priority task
                t.priority =
                    if (
                        builder.priority > Thread.MAX_PRIORITY
                        ||
                        builder.priority < Thread.MIN_PRIORITY
                    ) {
                    Thread.MIN_PRIORITY
                } else {
                    builder.priority
                }
                return t
            }
        },
        builder.exceptionHandler
    )

    fun <V> schedule(callable: Callable<V>, time: Long, timeUnit: TimeUnit): ScheduledFuture<V> {
        return backgroundPool.schedule(callable, time, timeUnit)
    }

    fun schedule(runnable: Runnable): ScheduledFuture<*> {
        return schedule(runnable, 0, TimeUnit.SECONDS)
    }

    fun schedule(runnable: Runnable, time: Long, timeUnit: TimeUnit): ScheduledFuture<*> {
        return backgroundPool.schedule(runnable, time, timeUnit)
    }

    fun scheduleAtFixedRate(
        task: Runnable,
        initialDelay: Long,
        period: Long,
        timeUnit: TimeUnit
    ): ScheduledFuture<*> {
        return backgroundPool.scheduleWithFixedDelay(task, initialDelay, period, timeUnit)
    }

    fun executor(): ScheduledThreadPoolExecutor {
        return backgroundPool
    }

    fun shutdown() {
        backgroundPool.shutdown()
    }

    override fun execute(command: Runnable) {
        backgroundPool.execute(command)
    }

    /**
     * Builder class for [ThreadPoolService]
     */
    class Builder(val name: String) {
        var poolSize: Int = 1
        var priority: Int = Thread.MIN_PRIORITY
        var exceptionHandler: ExceptionHandler? = null

        /**
         * @param poolSize the number of threads to keep in the pool
         * @throws IllegalArgumentException if size of pool <= 0
         */
        fun setPoolSize(poolSize: Int): Builder {
            if (poolSize <= 0) {
                throw IllegalArgumentException("Pool size must be greater than 0")
            }
            this.poolSize = poolSize
            return this
        }

        /**
         * @param priority Priority of the threads in the service. You can supply a constant from
         *                 [java.lang.Thread] or
         *                 specify your own priority in the range 1(MIN_PRIORITY)
         *                 to 10(MAX_PRIORITY)
         *                 By default, the priority is set to [java.lang.Thread.MIN_PRIORITY]
         */
        fun setPriority(priority: Int): Builder {
            this.priority = priority
            return this
        }

        /**
         * @param handler The handler to use to handle exceptions in the service
         */
        fun setExceptionHandler(handler: ExceptionHandler): Builder {
            exceptionHandler = handler
            return this
        }

        fun build(): ThreadPoolService {
            return ThreadPoolService(this)
        }
    }
}