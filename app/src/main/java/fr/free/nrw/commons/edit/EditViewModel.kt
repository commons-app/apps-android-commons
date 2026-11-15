package fr.free.nrw.commons.edit

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.ajpegtran.Jpegtran
import fr.free.nrw.commons.ajpegtran.Properties
import fr.free.nrw.commons.ajpegtran.blur.BlurRegion
import fr.free.nrw.commons.ajpegtran.rotate.RotationDegree
import timber.log.Timber
import java.io.File
import kotlin.math.ceil

/**
 * Largest width or height of the bitmap decoded for the preview.
 */
private const val MAX_PREVIEW_BITMAP_SIZE = 2000

/**
 * ViewModel for image editing operations using Jpegtran.
 */
class EditViewModel : ViewModel() {
    private var jpegtran: Jpegtran? = null

    /**
     * Path of the image being edited. It points to the latest transformed file.
     */
    var imagePath = ""
        private set

    /**
     * Initial EXIF orientation of the image being edited, in degrees.
     * It is reset to 0 once a pending rotation is applied to the file.
     */
    var startOrientation = 0
        private set

    /**
     * Rotation shown in the preview, in degrees. The difference from [startOrientation]
     * is applied to the file by [applyPendingRotation].
     */
    var imageRotation = 0

    /**
     * Properties of the image being edited, as last read by [refreshProperties].
     */
    var properties: Properties? = null
        private set

    /**
     * Width of the image file, as last read by [loadPreviewBitmap].
     */
    var originalBitmapWidth = 0
        private set

    /**
     * Height of the image file, as last read by [loadPreviewBitmap].
     */
    var originalBitmapHeight = 0
        private set

    /**
     * Initialize the single Jpegtran instance for this editing session.
     * Must be called with Application Context from EditActivity.
     * The session state is kept if it is already initialized, for example after the activity is recreated.
     */
    fun initJpegtran(
        context: Context,
        imagePath: String,
    ) {
        if (jpegtran == null) {
            val imageUri =
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://")) {
                    imagePath.toUri()
                } else {
                    File(imagePath).toUri()
                }
            jpegtran = Jpegtran(context, imageUri)
            this.imagePath = imagePath

            val sourceExif =
                runCatching { ExifInterface(imagePath) }
                    .recoverCatching {
                        context.contentResolver.openInputStream(imagePath.toUri())?.use {
                            ExifInterface(it)
                        }
                    }.onFailure { e ->
                        Timber.e(e, "Failed to read EXIF from URI")
                    }.getOrNull()

            // extracts the initial orientation
            val orientation =
                sourceExif?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            startOrientation =
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
        }
    }

    /**
     * Returns properties of the JPEG image being edited, and keeps them in [properties].
     */
    fun refreshProperties(): Properties {
        val currentProperties =
            jpegtran?.getProperties(File(imagePath).toUri())
                ?: error("Jpegtran not initialized")
        properties = currentProperties
        return currentProperties
    }

    /**
     * Clear and cleanup temporary files in Jpegtran.
     */
    fun cleanup() {
        jpegtran?.cleanup()
        jpegtran = null
    }

    override fun onCleared() {
        cleanup()
        super.onCleared()
    }

    /**
     * Rotates the specified image file by the given degree.
     * The rotated file becomes the current [imagePath].
     *
     * @param imageFile The File representing the image to be rotated.
     * @param degree The degree by which to rotate the image.
     * @param savePath The directory to save the rotated image in.
     * @return The rotated image File.
     */
    fun rotateImage(
        imageFile: File,
        degree: Int,
        savePath: File,
    ): File {
        Timber.tag("Trying to rotate image").d("Starting")
        val imagePath = "cropped_${System.currentTimeMillis()}.jpg"
        val output = File(savePath, imagePath)
        val normalizedDegree = ((degree % 360) + 360) % 360
        if (normalizedDegree == 0) {
            imageFile.copyTo(output, overwrite = true)
            this.imagePath = output.absolutePath
            return output
        }
        val rotationDegree =
            when (normalizedDegree) {
                90 -> RotationDegree.ROTATE_90
                180 -> RotationDegree.ROTATE_180
                270 -> RotationDegree.ROTATE_270
                else -> throw IllegalArgumentException("Unsupported degree: $degree")
            }
        try {
            jpegtran!!.rotate(rotationDegree)
            jpegtran!!.save(output.toUri())
            this.imagePath = output.absolutePath
            return output
        } catch (e: Exception) {
            Timber.e(e, "saveEditedImage: Failed to rotate image")
            throw e
        }
    }

    /**
     * Crops the specified image file using lossless JPEG cropping via Jpegtran.
     * The cropped file becomes the current [imagePath].
     *
     * @param left The left coordinate of the crop rectangle.
     * @param top The top coordinate of the crop rectangle.
     * @param width The width of the crop rectangle.
     * @param height The height of the crop rectangle.
     * @return The cropped image File.
     */
    fun cropImage(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        savePath: File,
    ): File {
        Timber.tag("Trying to crop image").d(
            "Starting crop: left=$left, top=$top, width=$width, height=$height",
        )
        val imagePath = "cropped_${System.currentTimeMillis()}.jpg"
        val output = File(savePath, imagePath)
        try {
            jpegtran!!.crop(
                width,
                height,
                left,
                top,
            )
            jpegtran!!.save(output.toUri())
            this.imagePath = output.absolutePath
            return output
        } catch (e: Exception) {
            Timber.e(e, "saveEditedImage: Failed to crop image")
            throw e
        }
    }

    /**
     * Blurs the specified regions of the image file.
     * The blurred file becomes the current [imagePath].
     *
     * @param regions The list of regions to blur.
     * @param savePath The directory to save the blurred image in.
     * @return The blurred image File.
     */
    fun blurImage(
        regions: List<BlurRegion>,
        savePath: File,
    ): File {
        Timber.tag("Trying to blur image").d("Starting blur of ${regions.size} regions")
        val imagePath = "blurred_${System.currentTimeMillis()}.jpg"
        val output = File(savePath, imagePath)
        try {
            jpegtran!!.blur(regions)
            jpegtran!!.save(output.toUri())
        } catch (e: Exception) {
            Timber.e(e, "blurImage: Failed to blur image")
            throw e
        }
        this.imagePath = output.absolutePath
        return output
    }

    /**
     * Applies any pending rotation to the given file via JNI and resets the rotation baseline.
     */
    fun applyPendingRotation(savePath: File) {
        val filePath = imagePath.toUri().path
        val file = File(filePath.toString())
        val relativeRotation = ((imageRotation - startOrientation) % 360 + 360) % 360
        if (relativeRotation == 0) return
        rotateImage(file, relativeRotation, savePath)
        // Reset rotation baseline since the file is now oriented correctly.
        startOrientation = 0
        imageRotation = 0
    }

    /**
     * Decodes the current [imagePath] for the preview, downsampling it when it is larger than
     * [MAX_PREVIEW_BITMAP_SIZE], and keeps the file size in [originalBitmapWidth] and [originalBitmapHeight].
     *
     * @return The decoded bitmap, or null if the file could not be decoded.
     */
    fun loadPreviewBitmap(): Bitmap? {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imagePath, options)

        val bitmapWidth = options.outWidth
        val bitmapHeight = options.outHeight
        originalBitmapWidth = bitmapWidth
        originalBitmapHeight = bitmapHeight

        val finalBitmap: Bitmap?
        if (bitmapWidth > MAX_PREVIEW_BITMAP_SIZE || bitmapHeight > MAX_PREVIEW_BITMAP_SIZE) {
            val scaleFactor = calculateScaleFactor(bitmapWidth, bitmapHeight, MAX_PREVIEW_BITMAP_SIZE)
            options.inSampleSize = scaleFactor
            options.inJustDecodeBounds = false
            finalBitmap = BitmapFactory.decodeFile(imagePath, options)
        } else {
            options.inJustDecodeBounds = false
            finalBitmap = BitmapFactory.decodeFile(imagePath, options)
        }
        return finalBitmap
    }
}

/**
 * Calculates the scale factor to be used for scaling down a bitmap based on its original
 *  dimensions and the maximum allowed size.
 * @param originalWidth  The original width of the bitmap.
 * @param originalHeight The original height of the bitmap.
 * @param maxSize        The maximum allowed size for either width or height.
 * @return The scale factor to be used for scaling down the bitmap.
 *         If the bitmap is smaller than or equal to the maximum size in both dimensions,
 *         the scale factor is 1.
 *         If the bitmap is larger than the maximum size in either dimension,
 *         the scale factor is calculated as the largest power of 2 that is less than or equal
 *         to the ratio of the original dimension to the maximum size.
 *         The scale factor ensures that the scaled bitmap will fit within the maximum size
 *         while maintaining aspect ratio.
 */
internal fun calculateScaleFactor(
    originalWidth: Int,
    originalHeight: Int,
    maxSize: Int,
): Int {
    var scaleFactor = 1

    if (originalWidth > maxSize || originalHeight > maxSize) {
        // Calculate the largest power of 2 that is less than or equal to the desired
        // width and height
        val widthRatio = ceil((originalWidth.toDouble() / maxSize.toDouble())).toInt()
        val heightRatio = ceil((originalHeight.toDouble() / maxSize.toDouble())).toInt()

        scaleFactor = if (widthRatio > heightRatio) widthRatio else heightRatio
    }

    return scaleFactor
}
