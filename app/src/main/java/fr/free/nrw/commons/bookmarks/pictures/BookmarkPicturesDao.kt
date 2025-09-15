package fr.free.nrw.commons.bookmarks.pictures

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.os.RemoteException
import androidx.core.content.contentValuesOf
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider.Companion.BASE_URI
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesContentProvider.Companion.uriForName
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.ALL_FIELDS
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.COLUMN_CREATOR
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable.COLUMN_MEDIA_NAME
import fr.free.nrw.commons.utils.getString
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class BookmarkPicturesDao @Inject constructor(
    @param:Named("bookmarks") private val clientProvider: Provider<ContentProviderClient>
) {
    /**
     * Find all persisted pictures bookmarks on database
     *
     * @return list of bookmarks
     */
    fun getAllBookmarks(): List<Bookmark> {
        val items: MutableList<Bookmark> = mutableListOf()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BASE_URI, ALL_FIELDS, null, arrayOf(), null
            )
            while (cursor != null && cursor.moveToNext()) {
                items.add(fromCursor(cursor))
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
     * Look for a bookmark in database and in order to insert or delete it
     *
     * @param bookmark : Bookmark object
     * @return boolean : is bookmark now fav ?
     */
    fun updateBookmark(bookmark: Bookmark): Boolean {
        val bookmarkExists = findBookmark(bookmark)
        if (bookmarkExists) {
            deleteBookmark(bookmark)
        } else {
            addBookmark(bookmark)
        }
        return !bookmarkExists
    }

    /**
     * Add a Bookmark to database
     *
     * @param bookmark : Bookmark to add
     */
    private fun addBookmark(bookmark: Bookmark) {
        val db = clientProvider.get()
        try {
            db.insert(BASE_URI, toContentValues(bookmark))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Delete a bookmark from database
     *
     * @param bookmark : Bookmark to delete
     */
    private fun deleteBookmark(bookmark: Bookmark) {
        val db = clientProvider.get()
        try {
            if (bookmark.contentUri == null) {
                throw RuntimeException("tried to delete item with no content URI")
            } else {
                db.delete(bookmark.contentUri!!, null, null)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Find a bookmark from database based on its name
     *
     * @param bookmark : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    fun findBookmark(bookmark: Bookmark?): Boolean {
        if (bookmark == null) {
            return false
        }

        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BASE_URI, ALL_FIELDS, "$COLUMN_MEDIA_NAME=?", arrayOf(bookmark.mediaName), null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return true
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return false
    }

    fun fromCursor(cursor: Cursor): Bookmark {
        val fileName = cursor.getString(COLUMN_MEDIA_NAME)
        return Bookmark(
            fileName, cursor.getString(COLUMN_CREATOR), uriForName(fileName)
        )
    }

    private fun toContentValues(bookmark: Bookmark): ContentValues = contentValuesOf(
        COLUMN_MEDIA_NAME to bookmark.mediaName,
        COLUMN_CREATOR to bookmark.mediaCreator
    )
}
