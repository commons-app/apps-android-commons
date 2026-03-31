package fr.free.nrw.commons.bookmarks.models

import android.net.Uri

class Bookmark(
    mediaName: String?,
    mediaCreator: String?,
    @Deprecated("Required for legacy ContentProvider DAO compatibility only")
    var contentUri: Uri? = null,
) {
    val mediaName: String = mediaName ?: ""
    val mediaCreator: String = mediaCreator ?: ""
}
