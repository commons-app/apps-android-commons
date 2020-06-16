package fr.free.nrw.commons.logging;

import android.os.Environment;
import fr.free.nrw.commons.upload.FileUtils;
import fr.free.nrw.commons.utils.ConfigUtils;

/**
 * Returns the log directory
 */
public final class LogUtils {

  private LogUtils() {
  }

  /**
   * Returns the directory for saving logs on the device
   *
   * @return
   */
  public static String getLogDirectory() {
    String dirPath;
    if (ConfigUtils.isBetaFlavour()) {
      dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          + "/logs/beta";
    } else {
      dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          + "/logs/prod";
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
      dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          + "/logs/beta/zip";
    } else {
      dirPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
          + "/logs/prod/zip";
    }

    FileUtils.recursivelyCreateDirs(dirPath);
    return dirPath;
  }
}
