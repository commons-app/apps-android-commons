package fr.free.nrw.commons.bookmarks.items

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import timber.log.Timber
import javax.inject.Inject

class BookmarkItemsContentProvider : CommonsDaggerContentProvider() {

    val BASE_PATH = "bookmarksItems"
    val BASE_URI : Uri =
        Uri.parse("content://" + BuildConfig.BOOKMARK_ITEMS_AUTHORITY + "/" + BASE_PATH)

    /**
     * Append bookmark locations name to the base uri
     */
    fun uriForName(name: String): Uri? {
        return Uri.parse("$BASE_URI/$name")
    }

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    override fun getType(uri: Uri): String? {
        return null
    }

    /**
     * Queries the SQLite database for the bookmark locations
     * @param uri : contains the uri for bookmark locations
     * @param projection
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     * @param sortOrder : ascending or descending
     */
    override fun query(
        uri: Uri, projection: Array<String?>?, selection: String?,
        selectionArgs: Array<String?>?, sortOrder: String?
    ): Cursor? {
        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = BookmarkItemsDao.Table.TABLE_NAME
        val db = dbOpenHelper.readableDatabase
        val cursor =
            queryBuilder.query(db, projection, selection,
                selectionArgs, null, null, sortOrder)
        cursor.setNotificationUri(context.contentResolver, uri)
        return cursor
    }

    /**
     * Handles the update query of local SQLite Database
     * @param uri : contains the uri for bookmark locations
     * @param contentValues : new values to be entered to db
     * @param selection : handles Where
     * @param selectionArgs : the condition of Where clause
     */
    override fun update(
        uri: Uri, contentValues: ContentValues?, selection: String?,
        selectionArgs: Array<String?>?
    ): Int {
        val sqlDB = dbOpenHelper.writableDatabase
        val rowsUpdated: Int = if (TextUtils.isEmpty(selection)) {
            val id = Integer.valueOf(uri.lastPathSegment)
            sqlDB.update(
                BookmarkItemsDao.Table.TABLE_NAME,
                contentValues,
                BookmarkItemsDao.Table.COLUMN_NAME + " = ?", arrayOf(id.toString())
            )
        } else {
            throw IllegalArgumentException(
                "Parameter `selection` should be empty when updating an ID"
            )
        }
        context.contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new bookmark locations record to local SQLite Database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val sqlDB = dbOpenHelper.writableDatabase
        val id = sqlDB.insert(BookmarkItemsDao.Table.TABLE_NAME, null, contentValues)
        context.contentResolver.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    override fun delete(uri: Uri, s: String?, strings: Array<String?>?): Int {
        val rows: Int
        val db = dbOpenHelper.readableDatabase
        Timber.d("Deleting bookmark name %s", uri.lastPathSegment)
        rows = db.delete(
            BookmarkItemsDao.Table.TABLE_NAME,
            "location_name = ?", arrayOf(uri.lastPathSegment)
        )
        context.contentResolver.notifyChange(uri, null)
        return rows
    }
}