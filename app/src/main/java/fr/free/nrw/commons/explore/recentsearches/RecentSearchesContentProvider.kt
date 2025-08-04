package fr.free.nrw.commons.explore.recentsearches

import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.net.Uri
import androidx.core.net.toUri
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.di.CommonsDaggerContentProvider
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.ALL_FIELDS
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.COLUMN_ID
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.TABLE_NAME

/**
 * This class contains functions for executing queries for
 * inserting, searching, deleting, editing recent searches in SqLite DB
 */
class RecentSearchesContentProvider : CommonsDaggerContentProvider() {

    /**
     * This functions executes query for searching recent searches in SqLite DB
     */
    override fun query(
        uri: Uri, projection: Array<String>?, selection: String?,
        selectionArgs: Array<String>?, sortOrder: String?
    ): Cursor {
        val queryBuilder = SQLiteQueryBuilder().apply {
            tables = TABLE_NAME
        }

        val uriType = uriMatcher.match(uri)

        val cursor = when (uriType) {
            RECENT_SEARCHES -> queryBuilder.query(
                requireDb(), projection, selection, selectionArgs,
                null, null, sortOrder
            )

            RECENT_SEARCHES_ID -> queryBuilder.query(
                requireDb(),
                ALL_FIELDS,
                "$COLUMN_ID = ?",
                arrayOf(uri.lastPathSegment),
                null,
                null,
                sortOrder
            )

            else -> throw IllegalArgumentException("Unknown URI$uri")
        }

        cursor.setNotificationUri(requireContext().contentResolver, uri)

        return cursor
    }

    override fun getType(uri: Uri): String? = null

    /**
     * This functions executes query for inserting a recentSearch object in SqLite DB
     */
    override fun insert(uri: Uri, contentValues: ContentValues?): Uri? {
        val uriType = uriMatcher.match(uri)
        val id: Long = when (uriType) {
            RECENT_SEARCHES -> requireDb().insert(TABLE_NAME, null, contentValues)

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        requireContext().contentResolver.notifyChange(uri, null)
        return "$BASE_URI/$id".toUri()
    }

    /**
     * This functions executes query for deleting a recentSearch object in SqLite DB
     */
    override fun delete(uri: Uri, s: String?, strings: Array<String>?): Int {
        val rows: Int
        val uriType = uriMatcher.match(uri)
        when (uriType) {
            RECENT_SEARCHES_ID -> {
                rows = requireDb().delete(
                    TABLE_NAME,
                    "_id = ?",
                    arrayOf(uri.lastPathSegment)
                )
            }

            else -> throw IllegalArgumentException("Unknown URI - $uri")
        }
        requireContext().contentResolver.notifyChange(uri, null)
        return rows
    }

    /**
     * This functions executes query for inserting multiple recentSearch objects in SqLite DB
     */
    override fun bulkInsert(uri: Uri, values: Array<ContentValues>): Int {
        val uriType = uriMatcher.match(uri)
        val sqlDB = requireDb()
        sqlDB.beginTransaction()
        when (uriType) {
            RECENT_SEARCHES -> for (value in values) {
                sqlDB.insert(TABLE_NAME, null, value)
            }

            else -> throw IllegalArgumentException("Unknown URI: $uri")
        }
        sqlDB.setTransactionSuccessful()
        sqlDB.endTransaction()
        requireContext().contentResolver.notifyChange(uri, null)
        return values.size
    }

    /**
     * This functions executes query for updating a particular recentSearch object in SqLite DB
     */
    override fun update(
        uri: Uri, contentValues: ContentValues?, selection: String?,
        selectionArgs: Array<String>?
    ): Int {
        /*
        SQL Injection warnings: First, note that we're not exposing this to the
        outside world (exported="false"). Even then, we should make sure to sanitize
        all user input appropriately. Input that passes through ContentValues
        should be fine. So only issues are those that pass in via concating.

        In here, the only concat created argument is for id. It is cast to an int,
        and will error out otherwise.
         */
        val uriType = uriMatcher.match(uri)
        val rowsUpdated: Int
        when (uriType) {
            RECENT_SEARCHES_ID -> if (selection.isNullOrEmpty()) {
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

            else -> throw IllegalArgumentException("Unknown URI: $uri with type $uriType")
        }
        requireContext().contentResolver.notifyChange(uri, null)
        return rowsUpdated
    }

    companion object {
        // For URI matcher
        private const val RECENT_SEARCHES = 1
        private const val RECENT_SEARCHES_ID = 2
        private const val BASE_PATH = "recent_searches"

        @JvmField
        val BASE_URI: Uri = "content://${BuildConfig.RECENT_SEARCH_AUTHORITY}/$BASE_PATH".toUri()

        private val uriMatcher = UriMatcher(UriMatcher.NO_MATCH)

        init {
            uriMatcher.addURI(BuildConfig.RECENT_SEARCH_AUTHORITY, BASE_PATH, RECENT_SEARCHES)
            uriMatcher.addURI(BuildConfig.RECENT_SEARCH_AUTHORITY, "$BASE_PATH/#", RECENT_SEARCHES_ID)
        }

        @JvmStatic
        fun uriForId(id: Int): Uri = "$BASE_URI/$id".toUri()
    }
}

