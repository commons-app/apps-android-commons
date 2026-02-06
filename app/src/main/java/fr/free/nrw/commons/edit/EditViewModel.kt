package fr.free.nrw.commons.edit

import androidx.lifecycle.ViewModel
import java.io.File

/**
 * ViewModel for image editing operations.
 *
 * This ViewModel class is responsible for managing image editing operations, such as
 * rotating images. It utilizes a TransformImage implementation to perform image transformations.
 */
class EditViewModel : ViewModel() {
    // Ideally should be injected using DI
    private val transformImage: TransformImage = TransformImageImpl()

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param degree The degree by which to rotate the image.
     * @param imageFile The File representing the image to be rotated.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(
        degree: Int,
        imageFile: File,
    ): File? = transformImage.rotateImage(imageFile, degree)

    /**
     * Crops the specified image file using lossless JPEG cropping.
     *
     * @param imageFile The File representing the image to be cropped.
     * @param left The left coordinate of the crop rectangle.
     * @param top The top coordinate of the crop rectangle.
     * @param width The width of the crop rectangle.
     * @param height The height of the crop rectangle.
     * @return The cropped image File, or null if the crop operation fails.
     */
    fun cropImage(
        imageFile: File,
        left: Int,
        top: Int,
        width: Int,
        height: Int,
    ): File? = transformImage.cropImage(imageFile, left, top, width, height)
}
