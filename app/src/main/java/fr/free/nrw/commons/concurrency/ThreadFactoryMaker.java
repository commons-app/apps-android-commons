package fr.free.nrw.commons.concurrency;

import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;

class ThreadFactoryMaker {
    public static ThreadFactory get(@NonNull final String name, final int priority) {
        return new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(final Runnable runnable) {
                count++;
                Runnable wrapperRunnable = new Runnable() {
                    @Override
                    public void run() {
                        Process.setThreadPriority(priority);
                        runnable.run();
                    }
                };
                Thread t = new Thread(wrapperRunnable, String.format("%s-%s", name, count));
                return t;
            }
        };
    }
}

