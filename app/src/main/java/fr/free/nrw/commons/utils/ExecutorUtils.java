package fr.free.nrw.commons.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;

public class ExecutorUtils {

    private static final Executor uiExecutor = new Executor() {
        @Override
        public void execute(Runnable command) {
            if (Looper.myLooper() == Looper.getMainLooper()) {
                command.run();
            } else {
                new Handler(Looper.getMainLooper()).post(command);
            }
        }
    };

    public static Executor uiExecutor () { return uiExecutor;}

}
