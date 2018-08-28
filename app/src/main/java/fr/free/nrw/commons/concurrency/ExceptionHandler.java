package fr.free.nrw.commons.concurrency;

import android.support.annotation.NonNull;

public interface ExceptionHandler {
    void onException(@NonNull Throwable t);
}
