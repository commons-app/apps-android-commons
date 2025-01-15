package fr.free.nrw.commons.filepicker

import android.content.ContentResolver
import android.content.Context
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import fr.free.nrw.commons.filepicker.Constants.Companion.DEFAULT_FOLDER_NAME
import timber.log.Timber
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


/**
 * PickedFiles.
 * Process the upload items.
 */
object PickedFiles : Constants {

    /**
     * Get Folder Name
     * @return default application folder name.
     */
    @JvmStatic
    private fun getFolderName(context: Context): String {
        return FilePicker.configuration(context).getFolderName()
    }

    /**
     * tempImageDirectory
     * @return temporary image directory to copy and perform exif changes.
     */
    @JvmStatic
    private fun tempImageDirectory(context: Context): File {
        val privateTempDir = File(context.cacheDir, DEFAULT_FOLDER_NAME)
        if (!privateTempDir.exists()) privateTempDir.mkdirs()
        return privateTempDir
    }

    /**
     * writeToFile
     * Writes inputStream data to the destination file.
     */
    @JvmStatic
    @Throws(IOException::class)
    private fun writeToFile(inputStream: InputStream, file: File) {
        inputStream.use { input ->
            FileOutputStream(file).use { output ->
                val buffer = ByteArray(1024)
                var length: Int
                while (input.read(buffer).also { length = it } > 0) {
                    output.write(buffer, 0, length)
                }
            }
        }
    }

    /**
     * Copy file function.
     * Copies source file to destination file.
     */
    @Throws(IOException::class)
    @JvmStatic
    private fun copyFile(src: File, dst: File) {
        FileInputStream(src).use { inputStream ->
            writeToFile(inputStream, dst)
        }
    }

    /**
     * Copy files in separate thread.
     * Copies all the uploadable files to the temp image folder on background thread.
     */
    @JvmStatic
    fun copyFilesInSeparateThread(context: Context, filesToCopy: List<UploadableFile>) {
        Thread {
            val copiedFiles = mutableListOf<File>()
            var index = 1
            filesToCopy.forEach { uploadableFile ->
                val fileToCopy = uploadableFile.file
                val dstDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                    getFolderName(context)
                )
                if (!dstDir.exists()) dstDir.mkdirs()

                val filenameSplit = fileToCopy.name.split(".")
                val extension = ".${filenameSplit.last()}"
                val filename = "IMG_${SimpleDateFormat(
                    "yyyyMMdd_HHmmss",
                    Locale.getDefault()).format(Date())}_$index$extension"
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
     * singleFileList
     * Converts a single uploadableFile to list of uploadableFile.
     */
    @JvmStatic
    fun singleFileList(file: UploadableFile): List<UploadableFile> {
        return listOf(file)
    }

    /**
     * ScanCopiedImages
     * Scans copied images metadata using media scanner.
     */
    @JvmStatic
    fun scanCopiedImages(context: Context, copiedImages: List<File>) {
        val paths = copiedImages.map { it.toString() }.toTypedArray()
        MediaScannerConnection.scanFile(context, paths, null) { path, uri ->
            Timber.d("Scanned $path:")
            Timber.d("-> uri=$uri")
        }
    }

    /**
     * pickedExistingPicture
     * Convert the image into uploadable file.
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    fun pickedExistingPicture(context: Context, photoUri: Uri): UploadableFile {
        val directory = tempImageDirectory(context)
        val mimeType = getMimeType(context, photoUri)
        val photoFile = File(directory, "${UUID.randomUUID()}.$mimeType")

        if (photoFile.createNewFile()) {
            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                writeToFile(inputStream, photoFile)
            }
        } else {
            throw IOException("Could not create photoFile to write upon")
        }
        return UploadableFile(photoUri, photoFile)
    }

    /**
     * getCameraPictureLocation
     */
    @Throws(IOException::class)
    @JvmStatic
    fun getCameraPicturesLocation(context: Context): File {
        val dir = tempImageDirectory(context)
        return File.createTempFile(UUID.randomUUID().toString(), ".jpg", dir)
    }

    /**
     * To find out the extension of the required object in a given uri
     */
    @JvmStatic
    private fun getMimeType(context: Context, uri: Uri): String {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver.getType(uri)
                ?.let { MimeTypeMapWrapper.getExtensionFromMimeType(it) }
        } else {
            MimeTypeMap.getFileExtensionFromUrl(
                Uri.fromFile(uri.path?.let { File(it) }).toString()
            )
        } ?: "jpg" // Default to jpg if unable to determine type
    }

    /**
     * GetUriToFile
     * @param file get uri of file
     * @return uri of requested file.
     */
    @JvmStatic
    fun getUriToFile(context: Context, file: File): Uri {
        val packageName = context.applicationContext.packageName
        val authority = "$packageName.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
