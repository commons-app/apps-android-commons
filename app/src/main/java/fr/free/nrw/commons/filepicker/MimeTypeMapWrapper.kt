package fr.free.nrw.commons.filepicker

import android.webkit.MimeTypeMap

object MimeTypeMapWrapper {
    private val sMimeTypeMap = MimeTypeMap.getSingleton()

    private val sMimeTypeToExtensionMap = mapOf(
        "image/heif" to "heif",
        "image/heic" to "heic"
    )

    @JvmStatic
    fun getExtensionFromMimeType(mimeType: String?): String? {
        val result = sMimeTypeToExtensionMap[mimeType]
        if (result != null) {
            return result
        }
        return sMimeTypeMap.getExtensionFromMimeType(mimeType)
    }
}
