package fr.free.nrw.commons.concurrency;

import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

class ExceptionAwareThreadPoolExecutor extends ScheduledThreadPoolExecutor {

    private final ExceptionHandler exceptionHandler;

    public ExceptionAwareThreadPoolExecutor(int corePoolSize, ThreadFactory threadFactory,
                                            ExceptionHandler exceptionHandler) {
        super(corePoolSize, threadFactory);
        this.exceptionHandler = exceptionHandler;
    }

    @Override
    protected void afterExecute(Runnable r, Throwable t) {
        super.afterExecute(r, t);
        if (t == null && r instanceof Future<?>) {
            try {
                Future<?> future = (Future<?>) r;
                if (future.isDone()) future.get();
            } catch (CancellationException | InterruptedException e) {
                //ignore
            } catch (ExecutionException e) {
                t = e.getCause() != null ? e.getCause() : e;
            } catch (Exception e) {
                t = e;
            }
        }

        if (t != null) {
            exceptionHandler.onException(t);
        }
    }
}
