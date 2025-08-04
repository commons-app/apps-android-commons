package fr.free.nrw.commons.explore.recentsearches

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.os.RemoteException
import androidx.core.content.contentValuesOf
import fr.free.nrw.commons.explore.models.RecentSearch
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider.Companion.BASE_URI
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesContentProvider.Companion.uriForId
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.ALL_FIELDS
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.COLUMN_ID
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.COLUMN_LAST_USED
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable.COLUMN_NAME
import fr.free.nrw.commons.utils.getInt
import fr.free.nrw.commons.utils.getLong
import fr.free.nrw.commons.utils.getString
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider

/**
 * This class doesn't execute queries in database directly instead it contains the logic behind
 * inserting, deleting, searching data from recent searches database.
 */
class RecentSearchesDao @Inject constructor(
    @param:Named("recentsearch") private val clientProvider: Provider<ContentProviderClient>
) {
    /**
     * This method is called on click of media/ categories for storing them in recent searches
     * @param recentSearch a recent searches object that is to be added in SqLite DB
     */
    fun save(recentSearch: RecentSearch) {
        val db = clientProvider.get()
        try {
            val contentValues = toContentValues(recentSearch)
            if (recentSearch.contentUri == null) {
                recentSearch.contentUri = db.insert(BASE_URI, contentValues)
            } else {
                db.update(recentSearch.contentUri!!, contentValues, null, null)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * This method is called on confirmation of delete recent searches.
     * It deletes all recent searches from the database
     */
    fun deleteAll() {
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BASE_URI,
                ALL_FIELDS,
                null,
                arrayOf(),
                "$COLUMN_LAST_USED DESC"
            )
            while (cursor != null && cursor.moveToNext()) {
                try {
                    val recentSearch = find(fromCursor(cursor).query)
                    if (recentSearch!!.contentUri == null) {
                        throw RuntimeException("tried to delete item with no content URI")
                    } else {
                        db.delete(recentSearch.contentUri!!, null, null)
                    }
                } catch (e: RemoteException) {
                    throw RuntimeException(e)
                } finally {
                    db.release()
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
        }
    }

    /**
     * Deletes a recent search from the database
     */
    fun delete(recentSearch: RecentSearch) {
        val db = clientProvider.get()
        try {
            if (recentSearch.contentUri == null) {
                throw RuntimeException("tried to delete item with no content URI")
            } else {
                db.delete(recentSearch.contentUri!!, null, null)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }


    /**
     * Find persisted search query in database, based on its name.
     * @param name Search query  Ex- "butterfly"
     * @return recently searched query from database, or null if not found
     */
    fun find(name: String): RecentSearch? {
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BASE_URI,
                ALL_FIELDS,
                "$COLUMN_NAME=?",
                arrayOf(name),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return fromCursor(cursor)
            }
        } catch (e: RemoteException) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return null
    }

    /**
     * Retrieve recently-searched queries, ordered by descending date.
     * @return a list containing recent searches
     */
    fun recentSearches(limit: Int): List<String> {
        val items: MutableList<String> = mutableListOf()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BASE_URI, ALL_FIELDS,
                null, arrayOf(), "$COLUMN_LAST_USED DESC"
            )
            // fixme add a limit on the original query instead of falling out of the loop?
            while (cursor != null && cursor.moveToNext() && cursor.position < limit) {
                items.add(fromCursor(cursor).query)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return items
    }

    /**
     * It creates an Recent Searches object from data stored in the SQLite DB by using cursor
     * @param cursor
     * @return RecentSearch object
     */
    fun fromCursor(cursor: Cursor): RecentSearch = RecentSearch(
        uriForId(cursor.getInt(COLUMN_ID)),
        cursor.getString(COLUMN_NAME),
        Date(cursor.getLong(COLUMN_LAST_USED))
    )

    /**
     * This class contains the database table architechture for recent searches,
     * It also contains queries and logic necessary to the create, update, delete this table.
     */
    private fun toContentValues(recentSearch: RecentSearch): ContentValues = contentValuesOf(
        COLUMN_NAME to recentSearch.query,
        COLUMN_LAST_USED to recentSearch.lastSearched.time
    )
}
