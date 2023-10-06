package fr.free.nrw.commons.edit

import androidx.lifecycle.ViewModel
import java.io.File

class EditViewModel() : ViewModel() {


    private val transformImage: TransformImage = TransformImageImpl()


    fun rotateImage(degree: Int, imageFile: File): File? {
        return transformImage.rotateImage(imageFile, degree)

    }
}