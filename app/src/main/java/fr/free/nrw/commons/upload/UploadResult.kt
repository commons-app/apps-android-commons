package fr.free.nrw.commons.upload

import org.wikipedia.gallery.ImageInfo

private const val RESULT_SUCCESS = "Sucess"

data class UploadResult(
    val result: String,
    val filekey: String,
    val filename: String,
    val sessionkey: String,
    val imageinfo: ImageInfo
) {
    fun isSuccessful(): Boolean = result == RESULT_SUCCESS

    fun createCanonicalFileName() = "File:$filename"
}
