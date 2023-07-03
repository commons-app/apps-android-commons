package fr.free.nrw.commons.edit

import java.io.File

interface TransformImage {

    fun rotateImage(imageFile: File, degree : Int ):File
}