package fr.free.nrw.commons.logging;

import android.content.Context;
import android.os.Environment;

/**
 * Returns the log directory
 */
public final class LogUtils {
    private LogUtils() {
    }

    /**
     * Returns the directory for saving logs on the device
     * @param isBeta
     * @return
     */
    public static String getLogDirectory(boolean isBeta) {
        if (isBeta) {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/logs/beta";
        } else {
            return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/logs/prod";
        }
    }
}
