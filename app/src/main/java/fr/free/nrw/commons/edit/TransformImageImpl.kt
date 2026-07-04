package fr.free.nrw.commons.edit

import android.content.Context
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import androidx.core.net.toUri
import fr.free.nrw.commons.ajpegtran.Jpegtran
import fr.free.nrw.commons.ajpegtran.Properties
import fr.free.nrw.commons.ajpegtran.rotate.RotationDegree
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Implementation of the TransformImage interface for image rotation and crop operations.
 *
 * This class provides an implementation for the TransformImage interface, exposing functions
 * for rotating and cropping images using the Jpegtran library for lossless JPEG transforms.
 */
class TransformImageImpl : TransformImage {

    private var jpegtran: Jpegtran? = null


    /**
     * Initialize the single Jpegtran instance for this editing session.
     */
    override fun initJpegtran(context: Context, imagePath: String) {
        if (jpegtran == null) {
            val imageUri =
                if (imagePath.startsWith("content://") || imagePath.startsWith("file://"))
                // It's a URI string, parse it directly.
                    imagePath.toUri()
                else
                // It's a raw file path, convert it safely.
                    File(imagePath).toUri()
            jpegtran = Jpegtran(
                context, imageUri
            )
        }
    }

    /**
     * Returns properties of the JPEG image.
     */
    override fun getProperties(uri: Uri): Properties {
        return jpegtran?.getProperties(uri)
            ?: throw IllegalStateException("Jpegtran not initialized")
    }

    override fun cleanup() {
        jpegtran?.cleanup()
        jpegtran = null
    }

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param imageFile The File representing the image to be rotated.
     * @param degree The degree by which to rotate the image.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    override fun rotateImage(
        imageFile: File,
        degree: Int,
        savePath: File
    ): File {
        Timber.tag("Trying to rotate image").d("Starting")
        val imagePath = System.currentTimeMillis()
        val output = File(savePath, "rotated_$imagePath.jpg")
        val normalizedDegree = ((degree % 360) + 360) % 360
        if (normalizedDegree == 0) {
            imageFile.copyTo(output, overwrite = true)
            return output
        }
        val rotationDegree = when (normalizedDegree) {
            90 -> RotationDegree.ROTATE_90
            180 -> RotationDegree.ROTATE_180
            270 -> RotationDegree.ROTATE_270
            else -> throw IllegalArgumentException("Unsupported degree: $degree")
        }
        try {
            jpegtran!!.rotate(rotationDegree)
            jpegtran!!.save(output.toUri())
            return output
        } catch (e: Exception) {
            Timber.e(e, "saveEditedImage: Failed to rotate image")
            throw e
        }
    }

    /**
     * Crops the specified image file using lossless JPEG cropping via Jpegtran.
     *
     * @param imageFile The File representing the image to be cropped.
     * @param left The left coordinate of the crop rectangle.
     * @param top The top coordinate of the crop rectangle.
     * @param width The width of the crop rectangle.
     * @param height The height of the crop rectangle.
     * @return The cropped image File, or null if the crop operation fails.
     */
    override fun cropImage(
        imageFile: File,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        savePath: File,
    ): File {
        Timber.tag("Trying to crop image").d(
            "Starting crop: left=$left, top=$top, width=$width, height=$height"
        )
        val imagePath = System.currentTimeMillis()
        val output = File(savePath, "cropped_$imagePath.jpg")
        try {
            jpegtran!!.crop(
                width,
                height,
                left,
                top
            )
            jpegtran!!.save(output.toUri())
            return output
        } catch (e: Exception) {
            Timber.e(e, "saveEditedImage: Failed to crop image")
            throw e
        }
    }
}
