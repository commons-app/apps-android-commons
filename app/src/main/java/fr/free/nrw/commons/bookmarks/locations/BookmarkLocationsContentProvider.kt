package fr.free.nrw.commons.bookmarks.locations

// We can get uri using java.Net.Uri, but android implementation is faster
// (but it's forgiving with handling exceptions though)
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao.Table.COLUMN_NAME
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao.Table.TABLE_NAME
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import timber.log.Timber
import javax.inject.Inject


/**
 * Handles private storage for Bookmark locations
 */
class BookmarkLocationsContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "bookmarksLocations"
        val BASE_URI: Uri =
            Uri.parse("content://${BuildConfig.BOOKMARK_LOCATIONS_AUTHORITY}/$BASE_PATH")

        /**
         * Append bookmark locations name to the base URI.
         */
        fun uriForName(name: String): Uri {
            return Uri.parse("$BASE_URI/$name")
        }
    }

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    override fun getType(uri: Uri): String? = null

    /**
     * Queries the SQLite database for the bookmark locations.
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
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
     * Handles the update query of local SQLite database.
     */
    override fun update(
        uri: Uri,
        contentValues: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        val db = dbOpenHelper.writableDatabase
        val rowsUpdated: Int

        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment?.toIntOrNull()
                ?: throw IllegalArgumentException("Invalid ID in URI")
            rowsUpdated = db.update(
                TABLE_NAME,
                contentValues,
                "$COLUMN_NAME = ?",
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
     * Handles the insertion of a new bookmark locations record to the local SQLite database.
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri {
        val db = dbOpenHelper.writableDatabase
        val id = db.insert(TABLE_NAME, null, contentValues)
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    /**
     * Handles the deletion of bookmark locations from the local SQLite database.
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
