package fr.free.nrw.commons.logging;

import android.os.Environment;

import fr.free.nrw.commons.BuildConfig;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.ConfigUtils;
import java.io.File;

/**
 * Returns the log directory
 */
public final class LogUtils {
    private LogUtils() {
    }

    private static final String PATH_BASE = Environment.getExternalStoragePublicDirectory(
        Environment.getDataDirectory().getAbsolutePath()
    ).getAbsolutePath();

    private static final String APPLICATION_ID = BuildConfig.APPLICATION_ID;

    private static final String APPLICATION_DIR = PATH_BASE + File.separator + APPLICATION_ID;

    private static final String LOGS_DIR_BETA = "/logs/beta";

    private static final String LOGS_DIR_PROD = "/logs/prod";

    private static final String ZIP = "zip";

    /**
     * Returns the directory for saving logs on the device
     *
     * @return
     */
    public static String getLogDirectory() {
        String dirPath;
        if (ConfigUtils.isBetaFlavour()) {
            dirPath = APPLICATION_DIR + LOGS_DIR_BETA;
        } else {
            dirPath = APPLICATION_DIR + LOGS_DIR_PROD;
        }

        FileUtils.recursivelyCreateDirs(dirPath);
        return dirPath;
    }

    /**
     * Returns the directory for saving logs on the device
     *
     * @return
     */
    public static String getLogZipDirectory() {
        String dirPath;
        if (ConfigUtils.isBetaFlavour()) {
            dirPath = APPLICATION_DIR + LOGS_DIR_BETA + File.separator + ZIP;
        } else {
            dirPath = APPLICATION_DIR + LOGS_DIR_PROD + File.separator + ZIP;
        }

        FileUtils.recursivelyCreateDirs(dirPath);
        return dirPath;
    }
}
