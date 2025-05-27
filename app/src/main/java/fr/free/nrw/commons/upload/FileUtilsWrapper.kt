package fr.free.nrw.commons.upload

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.upload.FileUtils.getMimeType
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileUtilsWrapper @Inject constructor(private val context: Context) {
    fun getSHA1(stream: InputStream?): String =
        stream?.let { FileUtils.getSHA1(it) } ?: ""

    @Throws(FileNotFoundException::class)
    fun getFileInputStream(filePath: String?): FileInputStream =
        FileUtils.getFileInputStream(filePath)

    fun getGeolocationOfFile(filePath: String, inAppPictureLocation: LatLng?): String? =
        FileUtils.getGeolocationOfFile(filePath, inAppPictureLocation)

    fun getMimeType(file: File?): String? =
        getMimeType(Uri.parse(file?.path))

    fun getMimeType(uri: Uri): String? =
        getMimeType(context, uri)

    /**
     * Takes a file as input and returns an Observable of files with the specified chunk size
     */
    @Throws(IOException::class)
    fun getFileChunks(file: File?, chunkSize: Int): List<File> {
        if (file == null) return emptyList()

        val buffer = ByteArray(chunkSize)

        FileInputStream(file).use { fis ->
            BufferedInputStream(fis).use { bis ->
                val buffers: MutableList<File> = ArrayList()
                var size: Int
                while ((bis.read(buffer).also { size = it }) > 0) {
                    buffers.add(
                        writeToFile(
                            buffer,
                            file.name ?: "",
                            getFileExt(file.name),
                            size
                        )
                    )
                }
                return buffers
            }
        }
    }

    private fun getFileExt(fileName: String): String =
        FileUtils.getFileExt(fileName)

    /**
     * Create a temp file containing the passed byte data.
     */
    @Throws(IOException::class)
    private fun writeToFile(data: ByteArray, fileName: String, fileExtension: String, size: Int): File {
        val file = File.createTempFile(fileName, fileExtension, context.cacheDir)
        try {
            if (!file.exists()) {
                file.createNewFile()
            }

            FileOutputStream(file).use { fos ->
                fos.write(data, 0, size)
            }
        } catch (throwable: Exception) {
            Timber.e(throwable, "Failed to create file")
        }
        return file
    }
}
