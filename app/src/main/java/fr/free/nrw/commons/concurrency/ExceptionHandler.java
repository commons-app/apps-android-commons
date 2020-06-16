package fr.free.nrw.commons.concurrency;

import androidx.annotation.NonNull;

public interface ExceptionHandler {

  void onException(@NonNull Throwable t);
}
