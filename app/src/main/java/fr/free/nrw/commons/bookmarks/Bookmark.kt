package fr.free.nrw.commons.bookmarks

import android.net.Uri

class Bookmark(mediaName: String?, mediaCreator: String?,
               /**
                * Modifies the content URI - marking this bookmark as already saved in the database
                * @param contentUri the content URI
                */
               var contentUri: Uri?) {
    /**
     * Gets the content URI for this bookmark
     * @return content URI
     */
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