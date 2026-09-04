package fr.free.nrw.commons.bookmarks.models

import android.net.Uri

class Bookmark(
    mediaName: String?,
    mediaCreator: String?,
) {
    val mediaName: String = mediaName ?: ""
    val mediaCreator: String = mediaCreator ?: ""
}
