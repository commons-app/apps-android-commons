package fr.free.nrw.commons.filepicker

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri

/**
 * Reports MIME types the way a gallery app does when it shares a RAW+JPEG pair, based on the
 * file name at the end of the uri.
 */
class FakeSharedFilesProvider : ContentProvider() {
    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String? =
        when (uri.lastPathSegment?.substringAfterLast('.')) {
            "jpg" -> "image/jpeg"
            "dng" -> "image/x-adobe-dng"
            else -> null
        }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun insert(
        uri: Uri,
        values: ContentValues?,
    ): Uri? = null

    override fun delete(
        uri: Uri,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?,
    ): Int = 0

    companion object {
        const val AUTHORITY = "fr.free.nrw.commons.test.sharedfiles"

        /**
         * Returns a uri served by this provider for the given file name.
         */
        fun uriFor(fileName: String): Uri = Uri.parse("content://$AUTHORITY/$fileName")
    }
}
