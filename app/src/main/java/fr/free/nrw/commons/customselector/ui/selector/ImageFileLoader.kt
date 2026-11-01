package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.provider.MediaStore
import android.text.format.DateFormat
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.model.Image
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

/**
 * Custom Selector Image File Loader.
 * Loads device images.
 */
class ImageFileLoader(
    val context: Context,
) : CoroutineScope {
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
            MediaStore.Images.Media.DATE_TAKEN,
        )

    /**
     * Load Device Images under coroutine.
     */
    fun loadDeviceImages(listener: ImageLoaderListener) {
        launch(Dispatchers.Main) {
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
            queryImageCursor() ?: run {
                listener.onFailed(NullPointerException("Cursor query returned null"))
                return
            }

        val indices = ImageColumnIndices(cursor)
        val images = extractImagesFromCursor(cursor, indices, listener) ?: return

        listener.onImageLoaded(images)
    }

    private fun queryImageCursor(): Cursor? =
        context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            MediaStore.Images.Media.DATE_TAKEN + " DESC",
        )

    private fun extractImagesFromCursor(
        cursor: Cursor,
        indices: ImageColumnIndices,
        listener: ImageLoaderListener,
    ): ArrayList<Image>? {
        val images = arrayListOf<Image>()
        val completed =
            cursor.use {
                readImagesFromCursor(it, indices, images, listener)
            }
        return if (completed) images else null
    }

    private fun readImagesFromCursor(
        cursor: Cursor,
        indices: ImageColumnIndices,
        images: ArrayList<Image>,
        listener: ImageLoaderListener,
    ): Boolean {
        if (cursor.moveToFirst()) {
            do {
                if (Thread.interrupted()) {
                    listener.onFailed(NullPointerException("Thread was interrupted during image loading"))
                    return false
                }
                parseAndAddImage(cursor, indices, images)
            } while (cursor.moveToNext())
        }
        return true
    }

    private data class ImageColumnIndices(
        val id: Int,
        val name: Int,
        val data: Int,
        val bucketId: Int,
        val bucketName: Int,
        val date: Int,
    ) {
        constructor(cursor: Cursor) : this(
            id = cursor.getColumnIndex(MediaStore.Images.Media._ID),
            name = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME),
            data = cursor.getColumnIndex(MediaStore.Images.Media.DATA),
            bucketId = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID),
            bucketName = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME),
            date = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED),
        )
    }

    private fun parseAndAddImage(
        cursor: Cursor,
        indices: ImageColumnIndices,
        images: ArrayList<Image>,
    ) {
        val path = cursor.getString(indices.data)
        val name = cursor.getString(indices.name)
        val bucketName = cursor.getString(indices.bucketName)

        if (path == null || name == null || bucketName == null) return
        if (!isValidImageFile(path)) return

        val id = cursor.getLong(indices.id)
        val bucketId = cursor.getLong(indices.bucketId)
        val dateVal = cursor.getLong(indices.date)

        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
        val formattedDate = formatImageDate(dateVal)

        images.add(
            Image(
                id = id,
                name = name,
                uri = uri,
                path = path,
                bucketId = bucketId,
                bucketName = bucketName,
                date = formattedDate,
            ),
        )
    }

    private fun isValidImageFile(path: String): Boolean {
        val file =
            try {
                File(path)
            } catch (ignored: Exception) {
                null
            }
        if (file == null || !file.exists()) return false

        val extension = path.substringAfterLast(".", "").lowercase(Locale.ROOT)
        val allowedExtensions =
            arrayOf(
                "jpg",
                "jpeg",
                "png",
                "svg",
                "gif",
                "tiff",
                "webp",
                "xcf",
            )
        return extension in allowedExtensions
    }

    private fun formatImageDate(dateMillis: Long): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = TimeUnit.SECONDS.toMillis(dateMillis)
        val dateFormat = DateFormat.getMediumDateFormat(context)
        return dateFormat.format(calendar.time)
    }

    /**
     * Abort loading images.
     */
    fun abortLoadImage() {
        // todo Abort loading images.
    }

    /*
     *
     * TODO
     * Sha1 for image (original image).
     *
     */
}
