package fr.free.nrw.commons.logging

import android.os.Environment

import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.ConfigUtils


/**
 * Returns the log directory
 */
object LogUtils {

    /**
     * Returns the directory for saving logs on the device.
     *
     * @return The path to the log directory.
     */
    fun getLogDirectory(): String {
        val dirPath = if (ConfigUtils.isBetaFlavour) {
            "${Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )}/logs/beta"
        } else {
            "${Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )}/logs/prod"
        }

        FileUtils.recursivelyCreateDirs(dirPath)
        return dirPath
    }

    /**
     * Returns the directory for saving zipped logs on the device.
     *
     * @return The path to the zipped log directory.
     */
    fun getLogZipDirectory(): String {
        val dirPath = if (ConfigUtils.isBetaFlavour) {
            "${Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )}/logs/beta/zip"
        } else {
            "${Environment
                .getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS
                )}/logs/prod/zip"
        }

        FileUtils.recursivelyCreateDirs(dirPath)
        return dirPath
    }
}