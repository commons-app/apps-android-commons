package fr.free.nrw.commons.concurrency;

import android.os.Process;
import android.support.annotation.NonNull;

import java.util.concurrent.ThreadFactory;

class ThreadFactoryMaker {
    public static ThreadFactory get(@NonNull final String name, final int priority) {
        return new ThreadFactory() {
            private int count = 0;

            @Override
            public Thread newThread(@NonNull final Runnable runnable) {
                count++;
                Runnable wrapperRunnable = () -> {
                    Process.setThreadPriority(priority);
                    runnable.run();
                };
                return new Thread(wrapperRunnable, String.format("%s-%s", name, count));
            }
        };
    }
}

