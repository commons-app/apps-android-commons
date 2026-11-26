package fr.free.nrw.commons.customselector.helper

import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import fr.free.nrw.commons.customselector.model.Image
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Metadata shown next to pictures in the list layout.
 */
data class ImageMetadata(
    val hasGeolocation: Boolean,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)

/**
 * Caches metadata for custom selector pictures so that list rows do not read EXIF data on the main thread.
 */
object ImageMetadataHelper {
    private val cache = ConcurrentHashMap<Long, ImageMetadata>()

    /**
     * Returns cached metadata for an image, if available.
     */
    fun get(image: Image): ImageMetadata? = cache[image.id]

    /**
     * Returns cached metadata or reads it from the file. Must be called off the main thread.
     */
    fun getOrLoad(image: Image): ImageMetadata = cache[image.id] ?: load(image).also { cache[image.id] = it }

    /**
     * Clears cached metadata, for example after the device images have been reloaded.
     */
    fun clear() {
        cache.clear()
    }

    private fun load(image: Image): ImageMetadata {
        var hasGeolocation = false
        var width = 0
        var height = 0

        try {
            val exif = ExifInterface(image.path)
            hasGeolocation = exif.getLatLong() != null
            width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
            height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
            if (width <= 0 || height <= 0) {
                val options =
                    BitmapFactory.Options().apply {
                        inJustDecodeBounds = true
                    }
                BitmapFactory.decodeFile(image.path, options)
                width = options.outWidth
                height = options.outHeight
            }
        } catch (ignored: IOException) {
            // Files without readable EXIF data simply have no metadata to display.
        } catch (ignored: IllegalArgumentException) {
            // Unsupported or malformed files simply have no metadata to display.
        }

        val sizeBytes =
            try {
                File(image.path).length()
            } catch (ignored: SecurityException) {
                0L
            }

        return ImageMetadata(hasGeolocation, width, height, sizeBytes)
    }
}
