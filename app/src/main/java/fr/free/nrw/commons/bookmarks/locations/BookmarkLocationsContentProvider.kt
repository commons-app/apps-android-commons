package fr.free.nrw.commons.bookmarks.locations

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder // We can get uri using java.Net.Uri, but andoid implimentation is faster (but it's forgiving with handling exceptions though)

// We can get uri using java.Net.Uri, but andoid implimentation is faster (but it's forgiving with handling exceptions though)
import android.net.Uri
import android.text.TextUtils

import androidx.annotation.NonNull

import javax.inject.Inject

import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao.Table.COLUMN_NAME
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao.Table.TABLE_NAME
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import timber.log.Timber


/**
 * Handles private storage for Bookmark locations
 */
class BookmarkLocationsContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "bookmarksLocations"
        val BASE_URI: Uri = Uri.parse("content://${BuildConfig.BOOKMARK_LOCATIONS_AUTHORITY}/$BASE_PATH")

        /**
         * Append bookmark locations name to the base URI
         */
        fun uriForName(name: String): Uri {
            return Uri.parse("$BASE_URI/$name")
        }
    }

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    override fun getType(uri: Uri): String? {
        return null
    }

    /**
     * Queries the SQLite database for the bookmark locations
     * @param uri : contains the URI for bookmark locations
     * @param projection
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     * @param sortOrder : ascending or descending
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        val db = dbOpenHelper.readableDatabase
        val cursor = queryBuilder.query(db, projection, selection, selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context?.contentResolver, uri)

        return cursor
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the URI for bookmark locations
     * @param contentValues : new values to be entered to DB
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val sqlDB = dbOpenHelper.writableDatabase
        val rowsUpdated: Int
        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment?.toInt() ?: throw IllegalArgumentException("Invalid ID in URI")
            rowsUpdated = sqlDB.update(
                TABLE_NAME,
                contentValues,
                "$COLUMN_NAME = ?",
                arrayOf(id.toString())
            )
        } else {
            throw IllegalArgumentException("Parameter `selection` should be empty when updating an ID")
        }
        context?.contentResolver?.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new bookmark locations record to local SQLite Database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri {
        val sqlDB = dbOpenHelper.writableDatabase
        val id = sqlDB.insert(BookmarkLocationsDao.Table.TABLE_NAME, null, contentValues)
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    /**
     * Handles the deletion of bookmark locations record from local SQLite Database
     */
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        val db = dbOpenHelper.readableDatabase
        Timber.d("Deleting bookmark name %s", uri.lastPathSegment)
        val rows = db.delete(
            TABLE_NAME,
            "location_name = ?",
            arrayOf(uri.lastPathSegment)
        )
        context?.contentResolver?.notifyChange(uri, null)
        return rows
    }
}
