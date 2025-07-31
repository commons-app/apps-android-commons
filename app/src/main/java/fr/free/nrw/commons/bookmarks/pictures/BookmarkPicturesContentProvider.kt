package fr.free.nrw.commons.bookmarks.pictures

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import androidx.core.net.toUri
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.COLUMN_MEDIA_NAME
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.TABLE_NAME

/**
 * Handles private storage for Bookmark pictures
 */
class BookmarkPicturesContentProvider : CommonsDaggerContentProvider() {
    override fun getType(uri: Uri): String? = null

    /**
     * Queries the SQLite database for the bookmark pictures
     * @param uri : contains the uri for bookmark pictures
     * @param projection
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     * @param sortOrder : ascending or descending
     */
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        val cursor = queryBuilder.query(
            requireDb(), projection, selection,
            selectionArgs, null, null, sortOrder
        )
        cursor.setNotificationUri(requireContext().contentResolver, uri)

        return cursor
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the uri for bookmark pictures
     * @param contentValues : new values to be entered to db
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    override fun update(
        uri: Uri, contentValues: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val rowsUpdated: Int
        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment!!.toInt()
            rowsUpdated = requireDb().update(
                TABLE_NAME,
                contentValues,
                "$COLUMN_MEDIA_NAME = ?",
                arrayOf(id.toString())
            )
        } else {
            throw IllegalArgumentException(
                "Parameter `selection` should be empty when updating an ID"
            )
        }
        requireContext().contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new bookmark pictures record to local SQLite Database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri {
        val id = requireDb().insert(TABLE_NAME, null, contentValues)
        requireContext().contentResolver.notifyChange(uri, null)
        return "$BASE_URI/$id".toUri()
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        val rows: Int = requireDb().delete(
            TABLE_NAME,
            "media_name = ?",
            arrayOf(uri.lastPathSegment)
        )
        requireContext().contentResolver.notifyChange(uri, null)
        return rows
    }

    companion object {
        private const val BASE_PATH = "bookmarks"
        @JvmField
        val BASE_URI: Uri = "content://${BuildConfig.BOOKMARK_AUTHORITY}/$BASE_PATH".toUri()

        @JvmStatic
        fun uriForName(name: String): Uri = "$BASE_URI/$name".toUri()
    }
}
