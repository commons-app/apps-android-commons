package fr.free.nrw.commons.edit

import java.io.File

/**
 * Interface for image transformation operations.
 *
 * This interface defines a contract for image transformation operations, allowing
 * implementations to provide specific functionality for tasks like rotating images.
 */
interface TransformImage {

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param imageFile The File representing the image to be rotated.
     * @param degree The degree by which to rotate the image.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(imageFile: File, degree : Int ):File?
}