package fr.free.nrw.commons.edit

import androidx.lifecycle.ViewModel
import java.io.File

/**
 * ViewModel for image editing operations.
 *
 * This ViewModel class is responsible for managing image editing operations, such as
 * rotating images. It utilizes a TransformImage implementation to perform image transformations.
 */
class EditViewModel() : ViewModel() {

    // Ideally should be injected using DI
    private val transformImage: TransformImage = TransformImageImpl()

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param degree The degree by which to rotate the image.
     * @param imageFile The File representing the image to be rotated.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(degree: Int, imageFile: File): File? {
        return transformImage.rotateImage(imageFile, degree)
    }
}