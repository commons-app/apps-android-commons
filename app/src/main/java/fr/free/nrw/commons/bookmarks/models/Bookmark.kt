package fr.free.nrw.commons.bookmarks.models

import android.net.Uri

class Bookmark(
    mediaName: String?,
    mediaCreator: String?,
    /**
     * Gets or Sets the content URI - marking this bookmark as already saved in the database
     * @return content URI
     * contentUri the content URI
     */
    var contentUri: Uri?,
) {
    /**
     * Gets the media name
     * @return the media name
     */
    val mediaName: String = mediaName ?: ""

    /**
     * Gets media creator
     * @return creator name
     */
    val mediaCreator: String = mediaCreator ?: ""
}
