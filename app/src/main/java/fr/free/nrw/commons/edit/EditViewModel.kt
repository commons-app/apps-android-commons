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
     * @param savePath The directory to save the rotated image in.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(
        degree: Int,
        imageFile: File,
        savePath: File): File? { return transformImage.rotateImage(imageFile, degree, savePath) }

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
        savePath: File,
    ): File? = transformImage.cropImage(imageFile, left, top, width, height, savePath)
}
