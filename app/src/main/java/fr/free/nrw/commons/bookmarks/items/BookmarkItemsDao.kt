package fr.free.nrw.commons.bookmarks.items

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.os.RemoteException
import androidx.core.content.contentValuesOf
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsContentProvider.Companion.BASE_URI
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsContentProvider.Companion.uriForName
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_CATEGORIES_DESCRIPTION_LIST
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_CATEGORIES_NAME_LIST
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_CATEGORIES_THUMBNAIL_LIST
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_DESCRIPTION
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_ID
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_IMAGE
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_INSTANCE_LIST
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_IS_SELECTED
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable.COLUMN_NAME
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.arrayToString
import fr.free.nrw.commons.utils.getString
import fr.free.nrw.commons.utils.getStringArray
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Handles database operations for bookmarked items
 */
@Singleton
class BookmarkItemsDao @Inject constructor(
    @param:Named("bookmarksItem") private val clientProvider: Provider<ContentProviderClient>
) {
    /**
     * Find all persisted items bookmarks on database
     * @return list of bookmarks
     */
    fun getAllBookmarksItems(): List<DepictedItem> {
        val items: MutableList<DepictedItem> = mutableListOf()
        val db = clientProvider.get()
        try {
            db.query(
                BASE_URI,
                BookmarkItemsTable.ALL_FIELDS,
                null,
                arrayOf(),
                null
            ).use { cursor ->
                while (cursor != null && cursor.moveToNext()) {
                    items.add(fromCursor(cursor))
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
        return items
    }


    /**
     * Look for a bookmark in database and in order to insert or delete it
     * @param depictedItem : Bookmark object
     * @return boolean : is bookmark now favorite ?
     */
    fun updateBookmarkItem(depictedItem: DepictedItem): Boolean {
        val bookmarkExists = findBookmarkItem(depictedItem.id)
        if (bookmarkExists) {
            deleteBookmarkItem(depictedItem)
        } else {
            addBookmarkItem(depictedItem)
        }
        return !bookmarkExists
    }

    /**
     * Add a Bookmark to database
     * @param depictedItem : Bookmark to add
     */
    private fun addBookmarkItem(depictedItem: DepictedItem) {
        val db = clientProvider.get()
        try {
            db.insert(BASE_URI, toContentValues(depictedItem))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Delete a bookmark from database
     * @param depictedItem : Bookmark to delete
     */
    private fun deleteBookmarkItem(depictedItem: DepictedItem) {
        val db = clientProvider.get()
        try {
            db.delete(uriForName(depictedItem.id), null, null)
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
    }

    /**
     * Find a bookmark from database based on its name
     * @param depictedItemID : Bookmark to find
     * @return boolean : is bookmark in database ?
     */
    fun findBookmarkItem(depictedItemID: String?): Boolean {
        if (depictedItemID == null) { //Avoiding NPE's
            return false
        }
        val db = clientProvider.get()
        try {
            db.query(
                BASE_URI,
                BookmarkItemsTable.ALL_FIELDS,
                COLUMN_ID + "=?",
                arrayOf(depictedItemID),
                null
            ).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    return true
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.release()
        }
        return false
    }

    /**
     * Recives real data from cursor
     * @param cursor : Object for storing database data
     * @return DepictedItem
     */
    @SuppressLint("Range")
    fun fromCursor(cursor: Cursor) = with(cursor) {
        DepictedItem(
            getString(COLUMN_NAME),
            getString(COLUMN_DESCRIPTION),
            getString(COLUMN_IMAGE),
            getStringArray(COLUMN_INSTANCE_LIST),
            convertToCategoryItems(
                getStringArray(COLUMN_CATEGORIES_NAME_LIST),
                getStringArray(COLUMN_CATEGORIES_DESCRIPTION_LIST),
                getStringArray(COLUMN_CATEGORIES_THUMBNAIL_LIST)
            ),
            getString(COLUMN_IS_SELECTED).toBoolean(),
            getString(COLUMN_ID)
        )
    }

    private fun convertToCategoryItems(
        categoryNameList: List<String>,
        categoryDescriptionList: List<String>,
        categoryThumbnailList: List<String>
    ): List<CategoryItem> {
        return buildList {
            for (i in categoryNameList.indices) {
                add(
                    CategoryItem(
                        categoryNameList[i],
                        categoryDescriptionList[i],
                        categoryThumbnailList[i],
                        false
                    )
                )
            }
        }
    }

    /**
     * Takes data from DepictedItem and create a content value object
     * @param depictedItem depicted item
     * @return ContentValues
     */
    private fun toContentValues(depictedItem: DepictedItem): ContentValues {
        return contentValuesOf(
            COLUMN_NAME to depictedItem.name,
            COLUMN_DESCRIPTION to depictedItem.description,
            COLUMN_IMAGE to depictedItem.imageUrl,
            COLUMN_INSTANCE_LIST to arrayToString(depictedItem.instanceOfs),
            COLUMN_CATEGORIES_NAME_LIST to arrayToString(depictedItem.commonsCategories.map { it.name }),
            COLUMN_CATEGORIES_DESCRIPTION_LIST to arrayToString(depictedItem.commonsCategories.map { it.description }),
            COLUMN_CATEGORIES_THUMBNAIL_LIST to arrayToString(depictedItem.commonsCategories.map { it.thumbnail }),
            COLUMN_IS_SELECTED to depictedItem.isSelected,
            COLUMN_ID to depictedItem.id,
        )
    }
}
