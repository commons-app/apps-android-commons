package fr.free.nrw.commons.bookmarks.pictures


import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import fr.free.nrw.commons.bookmarks.models.Bookmark
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton


@Singleton
class BookmarkPicturesDao @Inject constructor(
    @Named("bookmarks") private val clientProvider: Provider<ContentProviderClient>
) {

    /**
     * Find all persisted pictures bookmarks on database
     *
     * @return list of bookmarks
     */
    fun getAllBookmarks(): List<Bookmark> {
        val items = mutableListOf<Bookmark>()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkPicturesContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                null,
                emptyArray(),
                null
            )
            while (cursor?.moveToNext() == true) {
                items.add(fromCursor(cursor))
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.close()
        }
        return items
    }

    /**
     * Look for a bookmark in database and in order to insert or delete it
     *
     * @param bookmark : Bookmark object
     * @return boolean : is bookmark now fav?
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
            db.insert(BookmarkPicturesContentProvider.BASE_URI, toContentValues(bookmark))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
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
            val contentUri = bookmark.contentUri
            if (contentUri == null) {
                throw RuntimeException("Tried to delete item with no content URI")
            } else {
                db.delete(contentUri, null, null)
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    /**
     * Find a bookmark from database based on its name
     *
     * @param bookmark : Bookmark to find
     * @return boolean : is bookmark in database?
     */
    fun findBookmark(bookmark: Bookmark?): Boolean {
        if (bookmark == null) {
            // Avoiding NPEs
            return false
        }

        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkPicturesContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                "${Table.COLUMN_MEDIA_NAME} = ?",
                arrayOf(bookmark.mediaName),
                null
            )
            if (cursor?.moveToFirst() == true) {
                return true
            }
        } catch (e: RemoteException) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.close()
        }
        return false
    }

    @SuppressLint("Range")
    private fun fromCursor(cursor: Cursor): Bookmark {
        val fileName = cursor.getString(cursor.getColumnIndex(Table.COLUMN_MEDIA_NAME))
        return Bookmark(
            fileName,
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_CREATOR)),
            BookmarkPicturesContentProvider.uriForName(fileName)
        )
    }

    private fun toContentValues(bookmark: Bookmark): ContentValues {
        return ContentValues().apply {
            put(Table.COLUMN_MEDIA_NAME, bookmark.mediaName)
            put(Table.COLUMN_CREATOR, bookmark.mediaCreator)
        }
    }

    object Table {
        const val TABLE_NAME = "bookmarks"

        const val COLUMN_MEDIA_NAME = "media_name"
        const val COLUMN_CREATOR = "media_creator"

        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        val ALL_FIELDS = arrayOf(
            COLUMN_MEDIA_NAME,
            COLUMN_CREATOR
        )

        const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

        const val CREATE_TABLE_STATEMENT = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_MEDIA_NAME STRING PRIMARY KEY,
                $COLUMN_CREATOR STRING
            );
        """

        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
            if (from == to) return

            if (from < 7) {
                // doesn't exist yet
                onUpdate(db, from + 1, to)
                return
            }

            if (from == 7) {
                // table added in version 8
                onCreate(db)
                onUpdate(db, from + 1, to)
                return
            }

            if (from == 8) {
                onUpdate(db, from + 1, to)
            }
        }
    }
}
