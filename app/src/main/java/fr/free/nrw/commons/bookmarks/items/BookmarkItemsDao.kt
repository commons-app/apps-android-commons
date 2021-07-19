package fr.free.nrw.commons.bookmarks.items

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

@Singleton
class BookmarkItemsDao {

    private lateinit var clientProvider: Provider<ContentProviderClient>

    @Inject
    fun BookmarkItemsDao(@Named("bookmarksItem") clientProvider: Provider<ContentProviderClient>?) {
        this.clientProvider = clientProvider!!
    }


    /**
     * Find all persisted pictures bookmarks on database
     *
     * @return list of bookmarks
     */
    fun getAllBookmarksItems(): List<DepictedItem> {
        val items: MutableList<DepictedItem> = ArrayList()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkItemsContentProvider().BASE_URI,
                Table.ALL_FIELDS,
                null, arrayOf(),
                null
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
     * @param depictedItem : Bookmark object
     * @return boolean : is bookmark now fav ?
     */
    fun updateBookmarkItem(depictedItem: DepictedItem): Boolean {
        val bookmarkExists = findBookmarkItem(depictedItem)
        if (bookmarkExists) {
            deleteBookmarkItem(depictedItem)
        } else {
            addBookmarkItem(depictedItem)
        }
        return !bookmarkExists
    }

    /**
     * Add a Bookmark to database
     *
     * @param depictedItem : Bookmark to add
     */
    private fun addBookmarkItem(depictedItem: DepictedItem) {
        val db = clientProvider.get()
        try {
            db.insert(BookmarkItemsContentProvider().BASE_URI, toContentValues(depictedItem))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Delete a bookmark from database
     *
     * @param depictedItem : Bookmark to delete
     */
    private fun deleteBookmarkItem(depictedItem: DepictedItem) {
        val db = clientProvider.get()
        try {
            if (depictedItem.imageUrl == null) {
                throw RuntimeException("tried to delete item with no content URI")
            } else {
                db.delete(BookmarkItemsContentProvider().uriForName(depictedItem.name), null, null)
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
     * @param depictedItem : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    fun findBookmarkItem(depictedItem: DepictedItem?): Boolean {
        if (depictedItem == null) { //Avoiding NPE's
            return false
        }
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkItemsContentProvider().BASE_URI,
                Table.ALL_FIELDS,
                Table.COLUMN_NAME + "=?", arrayOf(depictedItem.name),
                null
            )
            if (cursor != null && cursor.moveToFirst()) {
                return true
            }
        } catch (e: RemoteException) {
            // This feels lazy, but to hell with checked exceptions. :)
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.release()
        }
        return false
    }

    fun fromCursor(cursor: Cursor): DepictedItem {
        val fileName =
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME))
        val description = cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION))
        val imageUrl = cursor.getString(cursor.getColumnIndex(Table.COLUMN_IMAGE))
        val instanceList = ArrayList<String>()
        val categoryList = ArrayList<String>()
        val isSelected = java.lang.Boolean.parseBoolean(cursor
            .getString(cursor.getColumnIndex(Table.COLUMN_IS_SELECTED)))
        val id = cursor.getString(cursor.getColumnIndex(Table.COLUMN_ID))

        return DepictedItem(
            fileName,
            description,
            imageUrl,
            instanceList,
            categoryList,
            isSelected,
            id
        )
    }

    private fun toContentValues(depictedItem: DepictedItem): ContentValues {
        val cv = ContentValues()
        cv.put(Table.COLUMN_NAME, depictedItem.name)
        cv.put(Table.COLUMN_DESCRIPTION, depictedItem.description)
        cv.put(Table.COLUMN_IMAGE, depictedItem.imageUrl)
        cv.put(Table.COLUMN_INSTANCE_LIST, depictedItem.instanceOfs.toString())
        cv.put(Table.COLUMN_CATEGORIES_LIST, depictedItem.commonsCategories.toString())
        cv.put(Table.COLUMN_IS_SELECTED, depictedItem.isSelected)
        cv.put(Table.COLUMN_ID, depictedItem.id)
        return cv
    }

    object Table {
        internal const val TABLE_NAME = "bookmarksItems"
        internal const val COLUMN_NAME = "item_name"
        internal const val COLUMN_DESCRIPTION = "item_description"
        internal const val COLUMN_IMAGE = "item_image_url"
        internal const val COLUMN_INSTANCE_LIST = "item_instance_of"
        internal const val COLUMN_CATEGORIES_LIST = "item_categories"
        internal const val COLUMN_IS_SELECTED = "item_is_selected"
        internal const val COLUMN_ID = "item_id"


        // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
        val ALL_FIELDS = arrayOf(
            COLUMN_NAME,
            COLUMN_DESCRIPTION,
            COLUMN_IMAGE,
            COLUMN_INSTANCE_LIST,
            COLUMN_CATEGORIES_LIST,
            COLUMN_IS_SELECTED,
            COLUMN_ID
        )
        const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"
        private const val CREATE_TABLE_STATEMENT = ("CREATE TABLE " + TABLE_NAME + " ("
                + COLUMN_NAME + " STRING PRIMARY KEY,"
                + COLUMN_DESCRIPTION + " STRING,"
                + COLUMN_IMAGE + " STRING,"
                + COLUMN_INSTANCE_LIST + " STRING,"
                + COLUMN_CATEGORIES_LIST + " STRING,"
                + COLUMN_IS_SELECTED + " STRING,"
                + COLUMN_ID + " STRING,"
                + ");")

        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        fun onUpdate(db: SQLiteDatabase?, from: Int, to: Int) {
            var from = from
            if (from == to) {
                return
            }
            if (from < 7) {
                // doesn't exist yet
                from++
                onUpdate(db, from, to)
                return
            }
            if (from == 7) {
                // table added in version 8
                db?.let { onCreate(it) }
                from++
                onUpdate(db, from, to)
                return
            }
            if (from == 8) {
                from++
                onUpdate(db, from, to)
                return
            }
        }
    }
}