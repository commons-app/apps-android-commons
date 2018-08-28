package fr.free.nrw.commons.logging;

import android.content.Context;
import android.os.Environment;

import timber.log.Timber;

public final class LogUtils {
    private LogUtils() {
    }

    /**
     * Returns the directory for saving logs on the device
     * @param context
     * @return
     */
    public static String getLogDirectory(Context context) {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/logs";
    }
}