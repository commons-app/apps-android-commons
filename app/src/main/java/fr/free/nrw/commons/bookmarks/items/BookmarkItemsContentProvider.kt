package fr.free.nrw.commons.bookmarks.items

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao.Table.COLUMN_ID
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao.Table.TABLE_NAME
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import timber.log.Timber
import javax.inject.Inject


/**
 * Handles private storage for bookmarked items
 */
class BookmarkItemsContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "bookmarksItems"
        val BASE_URI: Uri = Uri
            .parse("content://${BuildConfig.BOOKMARK_ITEMS_AUTHORITY}/$BASE_PATH")

        /**
         * Append bookmark items ID to the base URI
         */
        fun uriForName(id: String): Uri {
            return Uri.parse("$BASE_URI/$id")
        }
    }

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    override fun getType(uri: Uri): String? {
        return null
    }

    /**
     * Queries the SQLite database for the bookmark items
     * @param uri : contains the URI for bookmark items
     * @param projection : contains the all fields of the table
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     * @param sortOrder : ascending or descending
     */
    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }
        val db = dbOpenHelper.readableDatabase
        val cursor = queryBuilder.query(
            db,
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
        cursor.setNotificationUri(context?.contentResolver, uri)
        return cursor
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the URI for bookmark items
     * @param contentValues : new values to be entered to DB
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int {
        val sqlDB = dbOpenHelper.writableDatabase
        val rowsUpdated: Int

        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid ID in URI: $uri")
            rowsUpdated = sqlDB.update(
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

        context?.contentResolver?.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new bookmark items record to local SQLite Database
     * @param uri : contains the URI for bookmark items
     * @param contentValues : values to be inserted
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val sqlDB = dbOpenHelper.writableDatabase
        val id = sqlDB.insert(TABLE_NAME, null, contentValues)
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    /**
     * Handles the deletion of bookmark items record in the local SQLite Database
     * @param uri : contains the URI for bookmark items
     * @param selection : unused parameter
     * @param selectionArgs : unused parameter
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbOpenHelper.readableDatabase
        Timber.d("Deleting bookmark name %s", uri.lastPathSegment)
        val rows = db.delete(
            TABLE_NAME,
            "$COLUMN_ID = ?",
            arrayOf(uri.lastPathSegment)
        )
        context?.contentResolver?.notifyChange(uri, null)
        return rows
    }
}
