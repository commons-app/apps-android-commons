package fr.free.nrw.commons.customselector.ui.selector

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import fr.free.nrw.commons.customselector.listeners.ImageLoaderListener
import fr.free.nrw.commons.customselector.models.Image
import kotlinx.coroutines.*
import java.io.File
import kotlin.coroutines.CoroutineContext

/**
 * Custom Selector Image File Loader.
 * Loads device images.
 */
class ImageFileLoader(val context: Context) : CoroutineScope{

    /**
     * Coroutine context for fetching images.
     */
    override val coroutineContext: CoroutineContext = Dispatchers.Main

    /**
     * Media paramerters required.
     */
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.BUCKET_ID,
        MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

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
    private fun getImages(listener:ImageLoaderListener) {
        val cursor = context.contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")
        if (cursor == null) {
            listener.onFailed(NullPointerException())
            return
        }

        val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
        val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
        val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
        val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
        val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)

        val images = arrayListOf<Image>()
        if (cursor.moveToFirst()) {
            do {
                if (Thread.interrupted()) {
                    listener.onFailed(NullPointerException())
                    return
                }
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(dataColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)

                val file =
                    if (path == null || path.isEmpty()) {
                        null
                    } else try {
                        File(path)
                    } catch (ignored: Exception) {
                        null
                    }


                if (file != null && file.exists()) {
                    if (name != null && path != null && bucketName != null) {
                        val uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        val image = Image(id, name, uri, path, bucketId, bucketName)
                        images.add(image)
                    }
                }

            } while (cursor.moveToNext())
        }
        cursor.close()
        listener.onImageLoaded(images)
    }


    /**
     * Abort loading images.
     */
    fun abortLoadImage(){
        //todo Abort loading images.
    }

    /*
     *
     * TODO
     * Sha1 for image (original image).
     *
     */
}