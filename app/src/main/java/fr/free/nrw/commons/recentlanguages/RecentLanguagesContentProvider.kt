package fr.free.nrw.commons.recentlanguages


import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.COLUMN_NAME
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao.Table.TABLE_NAME
import androidx.core.net.toUri


/**
 * Content provider of recently used languages
 */
class RecentLanguagesContentProvider : CommonsDaggerContentProvider() {

    companion object {
        private const val BASE_PATH = "recent_languages"
        val BASE_URI: Uri = "content://${BuildConfig.RECENT_LANGUAGE_AUTHORITY}/$BASE_PATH".toUri()

        /**
         * Append language code to the base URI
         * @param languageCode Code of a language
         */
        @JvmStatic
        fun uriForCode(languageCode: String): Uri = "$BASE_URI/$languageCode".toUri()
    }

    override fun getType(uri: Uri): String? = null

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
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        val cursor = queryBuilder.query(
            requireDb(),
            projection,
            selection,
            selectionArgs,
            null,
            null,
            sortOrder
        )
        cursor.setNotificationUri(requireContext().contentResolver, uri)
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
        val rowsUpdated: Int
        if (selection.isNullOrEmpty()) {
            val id = uri.lastPathSegment?.toInt()
                ?: throw IllegalArgumentException("Invalid URI: $uri")
            rowsUpdated = requireDb().update(
                TABLE_NAME,
                contentValues,
                "$COLUMN_NAME = ?",
                arrayOf(id.toString())
            )
        } else {
            throw IllegalArgumentException("Parameter `selection` should be empty when updating an ID")
        }

        requireContext().contentResolver?.notifyChange(uri, null)
        return rowsUpdated
    }

    /**
     * Handles the insertion of new recently used languages record to local SQLite Database
     * @param uri : contains the URI for recently used languages
     * @param contentValues : new values to be entered to the database
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val id = requireDb().insert(
            TABLE_NAME,
            null,
            contentValues
        )
        requireContext().contentResolver?.notifyChange(uri, null)
        return "$BASE_URI/$id".toUri()
    }

    /**
     * Handles the deletion of a recently used languages record from local SQLite Database
     * @param uri : contains the URI for recently used languages
     */
    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        val rows = requireDb().delete(
            TABLE_NAME,
            "language_code = ?",
            arrayOf(uri.lastPathSegment)
        )
        requireContext().contentResolver?.notifyChange(uri, null)
        return rows
    }
}
