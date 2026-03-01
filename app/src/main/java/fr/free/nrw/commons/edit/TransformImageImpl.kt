package fr.free.nrw.commons.edit

import android.mediautil.image.jpeg.LLJTran
import android.mediautil.image.jpeg.LLJTranException
import android.os.Environment
import androidx.exifinterface.media.ExifInterface
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

/**
 * Implementation of the TransformImage interface for image rotation operations.
 *
 * This class provides an implementation for the TransformImage interface, right now it exposes a
 * function for rotating images by a specified degree using the LLJTran library. Right now it reads
 * the input image file, performs the rotation, and saves the rotated image to a new file.
 */
class TransformImageImpl : TransformImage {
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
    ): File? {
        Timber.tag("Trying to rotate image").d("Starting")

        val path =
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS,
            )

        val imagePath = System.currentTimeMillis()
        val output = File(savePath, "rotated_$imagePath.jpg")

        val rotated =
            try {
                val lljTran = LLJTran(imageFile)
                lljTran.read(
                    LLJTran.READ_ALL,
                    false,
                ) // This could throw an LLJTranException. I am not catching it for now... Let's see.
                lljTran.transform(
                    when (degree) {
                        90 -> LLJTran.ROT_90
                        180 -> LLJTran.ROT_180
                        270 -> LLJTran.ROT_270
                        else -> LLJTran.OPT_DEFAULTS
                    },
                    LLJTran.OPT_DEFAULTS or LLJTran.OPT_XFORM_ORIENTATION,
                )
                BufferedOutputStream(FileOutputStream(output)).use { writer ->
                    lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
                }
                lljTran.freeMemory()
                try {
                    val exif = ExifInterface(output.absolutePath)
                    exif.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
                    exif.saveAttributes()
                } catch (ex: Exception) {
                    Timber.w(ex, "Failed to force EXIF orientation to tha Normal")
                }
                true
            } catch (e: LLJTranException) {
                Timber.tag("Error").d(e)
                return null
            }

        if (rotated) {
            Timber.tag("Done rotating image").d("Done")
            Timber.tag("Add").d(output.absolutePath)
        }
        return output
    }
}
