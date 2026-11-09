package fr.free.nrw.commons.filepicker

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Copies picked files into the public Pictures folder of the app and makes them
 * visible to the media scanner.
 */
object PickedFilesCopier {
    /**
     * Get Folder Name
     * @return default application folder name.
     */
    @JvmStatic
    private fun getFolderName(context: Context): String = FilePicker.configuration(context).getFolderName()

    /**
     * Copy file function.
     * Copies source file to destination file.
     */
    @Throws(IOException::class)
    @JvmStatic
    private fun copyFile(
        src: File,
        dst: File,
    ) {
        FileInputStream(src).use { inputStream ->
            PickedFiles.writeToFile(inputStream, dst)
        }
    }

    /**
     * Copy files in separate thread.
     * Copies all the uploadable files to the temp image folder on background thread.
     */
    @JvmStatic
    fun copyFilesInSeparateThread(
        context: Context,
        filesToCopy: List<UploadableFile>,
    ) {
        Thread {
            val copiedFiles = mutableListOf<File>()
            var index = 1
            filesToCopy.forEach { uploadableFile ->
                val fileToCopy = uploadableFile.file
                val dstDir =
                    File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        getFolderName(context),
                    )
                if (!dstDir.exists()) dstDir.mkdirs()

                val filenameSplit = fileToCopy.name.split(".")
                val extension = ".${filenameSplit.last()}"
                val filename = "IMG_${SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault(),
                ).format(Date())}_$index$extension"
                val dstFile = File(dstDir, filename)

                try {
                    dstFile.createNewFile()
                    copyFile(fileToCopy, dstFile)
                    copiedFiles.add(dstFile)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
                index++
            }
            scanCopiedImages(context, copiedFiles)
        }.start()
    }

    /**
     * ScanCopiedImages
     * Scans copied images metadata using media scanner.
     */
    @JvmStatic
    fun scanCopiedImages(
        context: Context,
        copiedImages: List<File>,
    ) {
        val paths = copiedImages.map { it.toString() }.toTypedArray()
        MediaScannerConnection.scanFile(context, paths, null) { path, uri ->
            Timber.d("Scanned $path:")
            Timber.d("-> uri=$uri")
        }
    }
}
