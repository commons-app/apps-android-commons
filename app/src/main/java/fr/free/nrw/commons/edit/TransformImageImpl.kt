package fr.free.nrw.commons.edit

import android.mediautil.image.jpeg.LLJTran
import android.mediautil.image.jpeg.LLJTranException
import android.os.Environment
import timber.log.Timber
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream

class TransformImageImpl() : TransformImage {
    override fun rotateImage(imageFile: File): File {

        Timber.tag("Trying to rotate image with LLJTran").d("Starting")

        val path = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS
        )

        val imagePath = System.currentTimeMillis()
        val file: File = File(path, "$imagePath.jpg")

        val output = file

        val rotated = try {
            val lljTran = LLJTran(imageFile)
            lljTran.read(
                LLJTran.READ_ALL,
                false
            ) // This could throw an LLJTranException. I am not catching it for now... Let's see.
            lljTran.transform(
                LLJTran.ROT_180,
                LLJTran.OPT_DEFAULTS or LLJTran.OPT_XFORM_ORIENTATION
            )
            BufferedOutputStream(FileOutputStream(output)).use { writer ->
                lljTran.save(writer, LLJTran.OPT_WRITE_ALL)
            }
            lljTran.freeMemory()
            true
        } catch (e: LLJTranException) {
            Timber.tag("Error").d(e)
            false
        }

        if (rotated) {
            Timber.tag("Done rotating image").d("Done")
            Timber.tag("Add").d(output.absolutePath)
        }
        return output
    }

}
