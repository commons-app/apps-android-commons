package fr.free.nrw.commons.utils;

import android.os.Handler;
import android.os.Looper;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ExecutorUtils {

  private static final Executor uiExecutor = command -> {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      command.run();
    } else {
      new Handler(Looper.getMainLooper()).post(command);
    }
  };

  public static Executor uiExecutor() {
    return uiExecutor;
  }


  private static final ExecutorService executor = Executors.newFixedThreadPool(3);

  public static ExecutorService get() {
    return executor;
  }

}
