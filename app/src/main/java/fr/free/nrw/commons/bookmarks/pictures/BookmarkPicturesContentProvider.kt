package fr.free.nrw.commons.bookmarks.pictures


import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao.Table.COLUMN_MEDIA_NAME
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao.Table.TABLE_NAME
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import timber.log.Timber
import javax.inject.Inject


/**
 * Handles private storage for Bookmark pictures
 */
class BookmarkPicturesContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "bookmarks"
        val BASE_URI: Uri = Uri
            .parse("content://" + BuildConfig.BOOKMARK_AUTHORITY + "/" + BASE_PATH)

        /**
         * Append bookmark pictures name to the base uri
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
     * Queries the SQLite database for the bookmark pictures
     * @param uri : contains the uri for bookmark pictures
     * @param projection
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
    ): Cursor? {
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
     * @param uri : contains the uri for bookmark pictures
     * @param contentValues : new values to be entered to db
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
            val id = uri.lastPathSegment?.toInt()
                ?: throw IllegalArgumentException("Invalid ID in URI")
            rowsUpdated = sqlDB.update(
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

        context?.contentResolver?.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new bookmark pictures record to local SQLite Database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val sqlDB = dbOpenHelper.writableDatabase
        val id = sqlDB.insert(TABLE_NAME, null, contentValues)
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int {
        val db = dbOpenHelper.readableDatabase
        Timber.d("Deleting bookmark name %s", uri.lastPathSegment)

        val rows = db.delete(
            TABLE_NAME,
            "media_name = ?",
            arrayOf(uri.lastPathSegment)
        )

        context?.contentResolver?.notifyChange(uri, null)
        return rows
    }
}
