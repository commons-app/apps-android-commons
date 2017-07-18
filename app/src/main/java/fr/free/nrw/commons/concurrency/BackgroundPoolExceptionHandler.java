package fr.free.nrw.commons.concurrency;

import android.support.annotation.NonNull;

import fr.free.nrw.commons.BuildConfig;

public class BackgroundPoolExceptionHandler implements ExceptionHandler {
    @Override
    public void onException(@NonNull final Throwable t) {
        //Crash for debug build
        if (BuildConfig.DEBUG) {
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    throw new RuntimeException(t);
                }
            });
            thread.start();
        }
    }
}

