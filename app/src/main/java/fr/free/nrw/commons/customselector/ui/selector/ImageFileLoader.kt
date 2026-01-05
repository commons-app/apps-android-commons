package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.text.format.DateFormat
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.Image
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.coroutines.CoroutineContext

/**
 * Custom Selector Image File Loader.
 * Loads device images.
 */
class ImageFileLoader(
    val context: Context,
) : CoroutineScope {

    /**
     * keep track of the current loading job to allow cancellation.
     */
    private var loaderJob: Job? = null

    /**
     * Coroutine context for fetching images.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /**
     * Media paramerters required.
     */
    private val projection =
        arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
        )

    /**
     * Load Device Images under coroutine.
     */
    fun loadDeviceImages(listener: ImageLoaderListener) {
        loaderJob?.cancel()
        loaderJob = launch(Dispatchers.Main) {
            withContext(Dispatchers.IO) {
                getImages(listener)
            }
        }
    }

    /**
     * Load Device images using cursor
     */
    private fun getImages(listener: ImageLoaderListener) {
        val cursor =
            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC",
            )
        if (cursor == null) {
            listener.onFailed(NullPointerException())
            return
        }

        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
        val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)

        val images = arrayListOf<Image>()
        cursor.use { c -> //using .use automatically closes the cursor
            if (c.moveToFirst()) {
                do {
                    //check if coroutine was cancelled or thread interrupted
                    if (!isActive || Thread.interrupted()) {
                        return
                    }
                    val id = c.getLong(idColumn)
                    val name = c.getString(nameColumn)
                    val path = c.getString(dataColumn)
                    val bucketId = c.getLong(bucketIdColumn)
                    val bucketName = c.getString(bucketNameColumn)
                    val date = c.getLong(dateColumn)

                val file =
                    if (path == null || path.isEmpty()) {
                        null
                    } else {
                        try {
                            File(path)
                        } catch (ignored: Exception) {
                            null
                        }
                    }

                if (file != null && file.exists() && name != null && path != null && bucketName != null) {
                    val extension = path.substringAfterLast(".", "")
                    // Check if the extension is one of the allowed types
                    if (extension.lowercase(Locale.ROOT) !in arrayOf("jpg", "jpeg", "png", "svg",
                            "gif", "tiff", "webp", "xcf")) {
                        continue
                    }

                    val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                    val calendar = Calendar.getInstance()
                    calendar.timeInMillis = date * 1000L
                    val date: Date = calendar.time
                    val dateFormat = DateFormat.getMediumDateFormat(context)
                    val formattedDate = dateFormat.format(date)

                        val sha1 = getSha1(file)
                        val image =
                            Image(
                            id,
                            name,
                            uri,
                            path,
                            bucketId,
                            bucketName,
                            date = formattedDate,
                            sha1 = sha1
                        )
                        images.add(image)
                    }
                } while (c.moveToNext())
            }
        }
        //return results to the main thread via listener
        launch(Dispatchers.Main) {
            listener.onImageLoaded(images)
        }
    }

    /**
     * Abort loading images.
     */
    fun abortLoadImage() {
        loaderJob?.cancel()
    }

    /**
     * generates SHA-1 hash for the image file.
     */
    private fun getSha1(file: File): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-1")
            val fileInputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (fileInputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            val sha1Bytes = digest.digest()
            sha1Bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }
}
