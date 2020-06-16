package fr.free.nrw.commons.concurrency;

import androidx.annotation.NonNull;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

/**
 * This class is a thread pool which provides some additional features: - it sets the thread
 * priority to a value lower than foreground priority by default, or you can supply your own
 * priority - it gives you a way to handle exceptions thrown in the thread pool
 */

public class ThreadPoolService implements Executor {

  private final ScheduledThreadPoolExecutor backgroundPool;

  private ThreadPoolService(final Builder b) {
    backgroundPool = new ExceptionAwareThreadPoolExecutor(b.poolSize,
        new ThreadFactory() {
          int count = 0;

          @Override
          public Thread newThread(@NonNull Runnable r) {
            count++;
            Thread t = new Thread(r, String.format("%s-%s", b.name, count));
            //If the priority is specified out of range, we set the thread priority to Thread.MIN_PRIORITY
            //It's done prevent IllegalArgumentException and to prevent setting of improper high priority for a less priority task
            t.setPriority(b.priority > Thread.MAX_PRIORITY || b.priority < Thread.MIN_PRIORITY ?
                Thread.MIN_PRIORITY : b.priority);
            return t;
          }
        }, b.exceptionHandler);
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

  public ScheduledThreadPoolExecutor executor() {
    return backgroundPool;
  }

  public void shutdown() {
    backgroundPool.shutdown();
  }

  @Override
  public void execute(Runnable command) {
    backgroundPool.execute(command);
  }

  /**
   * Builder class for {@link ThreadPoolService}
   */
  public static class Builder {

    //Required
    private final String name;

    //Optional
    private int poolSize = 1;
    private int priority = Thread.MIN_PRIORITY;
    private ExceptionHandler exceptionHandler = null;

    /**
     * @param name the name of the threads in the service. if there are N threads, the thread names
     *             will be like name-1, name-2, name-3,...,name-N
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
     * @param priority Priority of the threads in the service. You can supply a constant from {@link
     *                 java.lang.Thread} or specify your own priority in the range 1(MIN_PRIORITY)
     *                 to 10(MAX_PRIORITY) By default, the priority is set to {@link
     *                 java.lang.Thread#MIN_PRIORITY}
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

    public ThreadPoolService build() {
      return new ThreadPoolService(this);
    }
  }
}
