package fr.free.nrw.commons.upload

import org.wikipedia.gallery.ImageInfo

private const val RESULT_SUCCESS = "Success"

data class UploadResult(
    val result: String,
    val filekey: String,
    val offset: Int,
    val filename: String,
    val sessionkey: String,
    val imageinfo: ImageInfo
) {
    fun isSuccessful(): Boolean = result == RESULT_SUCCESS

    fun createCanonicalFileName() = "File:$filename"
}
