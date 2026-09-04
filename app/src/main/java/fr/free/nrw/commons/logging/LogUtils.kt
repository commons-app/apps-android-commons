package fr.free.nrw.commons.logging

import android.content.Context

import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.utils.ConfigUtils


/**
 * Returns the log directory
 */
object LogUtils {

    /**
     * Returns the directory for saving logs on the device.
     *
     * Logs are written to app-scoped external storage so they do not clutter
     * the user's Downloads folder. Falls back to internal files dir if external
     * storage is unavailable.
     *
     * @return The path to the log directory.
     */
    fun getLogDirectory(context: Context): String {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dirPath = if (ConfigUtils.isBetaFlavour) {
            "$base/logs/beta"
        } else {
            "$base/logs/prod"
        }

        FileUtils.recursivelyCreateDirs(dirPath)
        return dirPath
    }

    /**
     * Returns the directory for saving zipped logs on the device.
     *
     * @return The path to the zipped log directory.
     */
    fun getLogZipDirectory(context: Context): String {
        val base = context.getExternalFilesDir(null) ?: context.filesDir
        val dirPath = if (ConfigUtils.isBetaFlavour) {
            "$base/logs/beta/zip"
        } else {
            "$base/logs/prod/zip"
        }

        FileUtils.recursivelyCreateDirs(dirPath)
        return dirPath
    }
}
