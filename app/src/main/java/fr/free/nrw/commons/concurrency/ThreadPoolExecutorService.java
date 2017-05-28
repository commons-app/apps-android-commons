package fr.free.nrw.commons.concurrency;

import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadPoolExecutorService implements Executor {
    private final ScheduledThreadPoolExecutor backgroundPool;

    private ThreadPoolExecutorService(Builder b) {
        backgroundPool = new ExceptionAwareThreadPoolExecutor(b.poolSize,
                ThreadFactoryMaker.get(b.name, b.priority), b.exceptionHandler);
    }

    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long time, TimeUnit timeUnit) {
        return backgroundPool.schedule(callable, time, timeUnit);
    }

    public ScheduledFuture<?> schedule(Runnable runnable) {
        return schedule(runnable, 0, TimeUnit.SECONDS);
    }

    public ScheduledFuture<?> schedule(Runnable runnable, long time, TimeUnit timeUnit) {
        return backgroundPool.schedule(runnable, time, timeUnit);
    }

    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable task, long initialDelay,
                                                  long period, final TimeUnit timeUnit) {
        return backgroundPool.scheduleAtFixedRate(task, initialDelay, period, timeUnit);
    }

    public void shutdown() {
        backgroundPool.shutdown();
    }

    @Override
    public void execute(Runnable command) {
        backgroundPool.execute(command);
    }

    /**
     * Builder class for {@link ThreadPoolExecutorService}
     */
    public static class Builder {
        //Required
        private final String name;

        //Optional
        private int poolSize = 1;
        private int priority = Process.THREAD_PRIORITY_BACKGROUND + Process.THREAD_PRIORITY_MORE_FAVORABLE;
        private ExceptionHandler exceptionHandler = null;

        /**
         * @param name the name of the threads in the service. if there are N threads,
         *             the thread names will be like name-1, name-2, name-3,...,name-N
         */
        public Builder(@NonNull String name) {
            this.name = name;
        }

        /**
         * @param poolSize the number of threads to keep in the pool
         * @throws IllegalArgumentException if size of pool <=0
         */
        public Builder setPoolSize(int poolSize) throws IllegalArgumentException {
            if (poolSize <= 0) {
                throw new IllegalArgumentException("Pool size must be grater than 0");
            }
            this.poolSize = poolSize;
            return this;
        }

        /**
         * @param priority Priority of the threads in the service. You can supply a constant from
         *                 {@link android.os.Process}
         *                 By default, the priority is set to a value slightly higher than the normal
         *                 background priority
         */
        public Builder setPriority(int priority) {
            this.priority = priority;
            return this;
        }

        /**
         * @param handler The handler to use to handle exceptions in the service
         */
        public Builder setExceptionHandler(ExceptionHandler handler) {
            this.exceptionHandler = handler;
            return this;
        }

        public ThreadPoolExecutorService build() {
            return new ThreadPoolExecutorService(this);
        }
    }
}
