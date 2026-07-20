package fr.free.nrw.commons.edit

import android.content.Context
import android.net.Uri
import fr.free.nrw.commons.ajpegtran.Properties
import java.io.File

/**
 * Interface for image transformation operations.
 *
 * This interface defines a contract for image transformation operations, allowing
 * implementations to provide specific functionality for tasks like rotating images.
 */
interface TransformImage {

    /**
     * Initialize the single Jpegtran instance for this editing session.
     */
    fun initJpegtran(context: Context, imagePath: String)

    /**
     * Returns properties of the JPEG image.
     */
    fun getProperties(uri: Uri): Properties

    /**
     * Clear and cleanup temporary files in Jpegtran.
     */
    fun cleanup()

    /**
     * Rotates the specified image file by the given degree.
     *
     * @param imageFile The File representing the image to be rotated.
     * @param degree The degree by which to rotate the image.
     * @param savePath The directory to save the rotated image in.
     * @return The rotated image File, or null if the rotation operation fails.
     */
    fun rotateImage(
        imageFile: File,
        degree: Int,
        savePath: File
    ): File

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
    ): File
}
