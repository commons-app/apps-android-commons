package fr.free.nrw.commons.recentlanguages


import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import android.text.TextUtils
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.COLUMN_NAME
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.TABLE_NAME
import javax.inject.Inject
import timber.log.Timber


/**
 * Content provider of recently used languages
 */
class RecentLanguagesContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "recent_languages"
        val BASE_URI: Uri =
            Uri.parse(
                "content://${BuildConfig.RECENT_LANGUAGE_AUTHORITY}/$BASE_PATH"
            )

        /**
         * Append language code to the base URI
         * @param languageCode Code of a language
         */
        @JvmStatic
        fun uriForCode(languageCode: String): Uri {
            return Uri.parse("$BASE_URI/$languageCode")
        }
    }

    @Inject
    lateinit var dbOpenHelper: DBOpenHelper

    override fun getType(uri: Uri): String? {
        return null
    }

    /**
     * Queries the SQLite database for the recently used languages
     * @param uri : contains the URI for recently used languages
     * @param projection : contains all fields of the table
     * @param selection : handles WHERE
     * @param selectionArgs : the condition of WHERE clause
     * @param sortOrder : ascending or descending
     */
    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? {
        val queryBuilder = SQLiteQueryBuilder()
        queryBuilder.tables = TABLE_NAME
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
     * @param uri : contains the URI for recently used languages
     * @param contentValues : new values to be entered to the database
     * @param selection : handles WHERE
     * @param selectionArgs : the condition of WHERE clause
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
            val id = uri.lastPathSegment?.toInt()
                ?: throw IllegalArgumentException("Invalid URI: $uri")
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
     * Handles the insertion of new recently used languages record to local SQLite Database
     * @param uri : contains the URI for recently used languages
     * @param contentValues : new values to be entered to the database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val sqlDB = dbOpenHelper.writableDatabase
        val id = sqlDB.insert(
            TABLE_NAME,
            null,
            contentValues
        )
        context?.contentResolver?.notifyChange(uri, null)
        return Uri.parse("$BASE_URI/$id")
    }

    /**
     * Handles the deletion of a recently used languages record from local SQLite Database
     * @param uri : contains the URI for recently used languages
     */
    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        val db = dbOpenHelper.readableDatabase
        Timber.d("Deleting recently used language %s", uri.lastPathSegment)
        val rows = db.delete(
            TABLE_NAME,
            "language_code = ?",
            arrayOf(uri.lastPathSegment)
        )
        context?.contentResolver?.notifyChange(uri, null)
        return rows
    }
}
