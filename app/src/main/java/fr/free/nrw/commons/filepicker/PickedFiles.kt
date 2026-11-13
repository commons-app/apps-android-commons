package fr.free.nrw.commons.filepicker

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.filepicker.Constants.Companion.DEFAULT_FOLDER_NAME
import fr.free.nrw.commons.utils.RandomAccessFileExifWriter
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * PickedFiles.
 * Process the upload items.
 */
object PickedFiles : Constants {
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
    internal fun writeToFile(
        inputStream: InputStream,
        file: File,
    ) {
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
     * singleFileList
     * Converts a single uploadableFile to list of uploadableFile.
     */
    @JvmStatic
    fun singleFileList(file: UploadableFile): List<UploadableFile> = listOf(file)

    /**
     * pickedExistingPicture
     * Convert the image into uploadable file.
     */
    @Throws(IOException::class, SecurityException::class)
    @JvmStatic
    fun pickedExistingPicture(
        context: Context,
        photoUri: Uri,
    ): UploadableFile {
        val directory = tempImageDirectory(context)
        val ext = getMimeType(context, photoUri)
        val isHeic = ext.equals("heic", true) || ext.equals("heif", true)
        val photoFile = File(directory, "${UUID.randomUUID()}.${if (isHeic) "jpg" else ext}")

        if (isHeic) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
                throw IOException("HEIC not supported on this Android version")
            }
            val srcExif = context.contentResolver.openInputStream(photoUri)?.use { ExifInterface(it) }
            HeicApi28.transcodeToJpeg(context.contentResolver, photoUri, photoFile)
            srcExif?.let { copyExif(it, photoFile) }
            return UploadableFile(photoUri, photoFile).apply {
                hasUnsupportedFormat = true
            }
        }

        if (photoFile.createNewFile()) {
            context.contentResolver.openInputStream(photoUri)?.use { inputStream ->
                writeToFile(inputStream, photoFile)
            }
        } else {
            throw IOException("Could not create photoFile to write upon")
        }
        return UploadableFile(photoUri, photoFile)
    }

    private fun copyExif(
        src: ExifInterface,
        dstFile: File,
    ) {
        RandomAccessFileExifWriter.copyExif(src, dstFile)
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private object HeicApi28 {
        fun transcodeToJpeg(
            cr: ContentResolver,
            uri: Uri,
            outFile: File,
        ) {
            val src = android.graphics.ImageDecoder.createSource(cr, uri)
            val bmp = android.graphics.ImageDecoder.decodeBitmap(src)
            FileOutputStream(outFile).use { bmp.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        }
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
    private fun getMimeType(
        context: Context,
        uri: Uri,
    ): String {
        return if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
            context.contentResolver
                .getType(uri)
                ?.let { MimeTypeMapWrapper.getExtensionFromMimeType(it) }
        } else {
            MimeTypeMap.getFileExtensionFromUrl(
                Uri.fromFile(uri.path?.let { File(it) }).toString(),
            )
        } ?: "jpg" // Default to jpg if unable to determine type
    }

    /**
     * GetUriToFile
     * @param file get uri of file
     * @return uri of requested file.
     */
    @JvmStatic
    fun getUriToFile(
        context: Context,
        file: File,
    ): Uri {
        val packageName = context.applicationContext.packageName
        val authority = "$packageName.provider"
        return FileProvider.getUriForFile(context, authority, file)
    }
}
