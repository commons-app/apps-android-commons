package fr.free.nrw.commons.bookmarks.items

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.TABLE_NAME
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import androidx.core.net.toUri
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_ID

/**
 * Handles private storage for bookmarked items
 */
class BookmarkItemsContentProvider : CommonsDaggerContentProvider() {
    override fun getType(uri: Uri): String? = null

    /**
     * Queries the SQLite database for the bookmark items
     * @param uri : contains the uri for bookmark items
     * @param projection : contains the all fields of the table
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

        return queryBuilder.query(
            requireDb(), projection, selection,
            selectionArgs, null, null, sortOrder
        ).apply {
            setNotificationUri(requireContext().contentResolver, uri)
        }
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the uri for bookmark items
     * @param contentValues : new values to be entered to db
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    override fun update(
        uri: Uri, contentValues: ContentValues?,
        selection: String?, selectionArgs: Array<String>?
    ): Int {
        val rowsUpdated: Int
        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment!!.toInt()
            rowsUpdated = requireDb().update(
                TABLE_NAME,
                contentValues,
                "$COLUMN_ID = ?",
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
     * Handles the insertion of new bookmark items record to local SQLite Database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val id = requireDb().insert(TABLE_NAME, null, contentValues)
        requireContext().contentResolver.notifyChange(uri, null)
        return "$BASE_URI/$id".toUri()
    }


    /**
     * Handles the deletion of new bookmark items record to local SQLite Database
     */
    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        val rows: Int = requireDb().delete(
            TABLE_NAME,
            "$COLUMN_ID = ?",
            arrayOf(uri.lastPathSegment)
        )
        requireContext().contentResolver.notifyChange(uri, null)
        return rows
    }

    companion object {
        private const val BASE_PATH = "bookmarksItems"
        val BASE_URI: Uri = "content://${BuildConfig.BOOKMARK_ITEMS_AUTHORITY}/$BASE_PATH".toUri()
        fun uriForName(id: String) = "$BASE_URI/$id".toUri()
    }
}
