package fr.free.nrw.commons.edit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.ajpegtran.Properties
import java.io.File

/**
 * ViewModel for image editing operations.
 */
class EditViewModel : ViewModel() {
    // Ideally should be injected using DI
    private val transformImage: TransformImage = TransformImageImpl()

    /**
     * Initialize the single Jpegtran instance for this editing session.
     * Must be called with Application Context from EditActivity.
     */
    fun initJpegtran(context: Context, imagePath: String) {
        transformImage.initJpegtran(context, imagePath)
    }

    /**
     * Returns properties of the JPEG image.
     */
    fun getProperties(uri: Uri): Properties {
        return transformImage.getProperties(uri)
    }

    /**
     * Clear and cleanup temporary files in Jpegtran.
     */
    override fun onCleared() {
        // If user canceled/backed or finishes editing, clear the temporary files in the library.
        transformImage.cleanup()
        super.onCleared()
    }

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param degree The degree by which to rotate the image.
     * @param imageFile The File representing the image to be rotated.
     * @param savePath The directory to save the rotated image in.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(
        imageFile: File,
        degree: Int,
        savePath: File
    ): File {
        return transformImage.rotateImage(imageFile, degree, savePath)
    }

    /**
     * Crops the specified image file using lossless JPEG cropping.
     *
     * @param left The left coordinate of the crop rectangle.
     * @param top The top coordinate of the crop rectangle.
     * @param width The width of the crop rectangle.
     * @param height The height of the crop rectangle.
     * @return The cropped image File, or null if the crop operation fails.
     */
    fun cropImage(
        left: Int,
        top: Int,
        width: Int,
        height: Int,
        savePath: File,
    ): File = transformImage.cropImage(left, top, width, height, savePath)
}
