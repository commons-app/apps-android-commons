package fr.free.nrw.commons.upload

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.location.LatLng
import timber.log.Timber
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.math.BigInteger
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Locale

object FileUtils {

    /**
     * Get SHA1 of filePath from input stream
     */
    fun getSHA1(stream: InputStream): String {
        val digest: MessageDigest
        try {
            digest = MessageDigest.getInstance("SHA1")
        } catch (e: NoSuchAlgorithmException) {
            Timber.e(e, "Exception while getting Digest")
            return ""
        }

        val buffer = ByteArray(8192)
        var read: Int
        try {
            while ((stream.read(buffer).also { read = it }) > 0) {
                digest.update(buffer, 0, read)
            }
            val md5sum = digest.digest()
            val bigInt = BigInteger(1, md5sum)
            var output = bigInt.toString(16)
            // Fill to 40 chars
            output = String.format("%40s", output).replace(' ', '0')
            Timber.i("File SHA1: %s", output)

            return output
        } catch (e: IOException) {
            Timber.e(e, "IO Exception")
            return ""
        } finally {
            try {
                stream.close()
            } catch (e: IOException) {
                Timber.e(e, "Exception on closing MD5 input stream")
            }
        }
    }

    /**
     * Get Geolocation of filePath from input filePath path
     */
    fun getGeolocationOfFile(filePath: String, inAppPictureLocation: LatLng?): String? = try {
        val exifInterface = ExifInterface(filePath)
        val imageObj = ImageCoordinates(exifInterface, inAppPictureLocation)
        if (imageObj.decimalCoords != null) { // If image has geolocation information in its EXIF
            imageObj.decimalCoords
        } else {
            ""
        }
    } catch (e: IOException) {
        Timber.e(e)
        ""
    }


    /**
     * Read and return the content of a resource filePath as string.
     *
     * @param fileName asset filePath's path (e.g. "/queries/radius_query_for_upload_wizard.rq")
     * @return the content of the filePath
     */
    @Throws(IOException::class)
    fun readFromResource(fileName: String) = buildString {
        try {
            val inputStream = FileUtils::class.java.getResourceAsStream(fileName) ?:
                throw FileNotFoundException(fileName)

            BufferedReader(InputStreamReader(inputStream, "UTF-8")).use { reader ->
                var line: String?
                while ((reader.readLine().also { line = it }) != null) {
                    append(line).append("\n")
                }
            }
        } catch (e: Throwable) {
            Timber.e(e)
        }
    }

    /**
     * Deletes files.
     *
     * @param file context
     */
    fun deleteFile(file: File?): Boolean {
        var deletedAll = true
        if (file != null) {
            if (file.isDirectory) {
                val children = file.list()
                for (child in children!!) {
                    deletedAll = deleteFile(File(file, child)) && deletedAll
                }
            } else {
                deletedAll = file.delete()
            }
        }

        return deletedAll
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        val mimeType: String?
        if (uri.scheme != null && uri.scheme == ContentResolver.SCHEME_CONTENT) {
            val cr = context.contentResolver
            mimeType = cr.getType(uri)
        } else {
            val fileExtension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                fileExtension.lowercase(Locale.getDefault())
            )
        }
        return mimeType
    }

    fun getFileExt(fileName: String): String {
        //Default filePath extension
        var extension = ".jpg"

        val i = fileName.lastIndexOf('.')
        if (i > 0) {
            extension = fileName.substring(i + 1)
        }
        return extension
    }

    @Throws(FileNotFoundException::class)
    fun getFileInputStream(filePath: String?): FileInputStream =
        FileInputStream(filePath)

    fun recursivelyCreateDirs(dirPath: String): Boolean {
        val fileDir = File(dirPath)
        if (!fileDir.exists()) {
            return fileDir.mkdirs()
        }
        return true
    }
}
