package fr.free.nrw.commons.bookmarks.items

import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.os.RemoteException
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider
import javax.inject.Singleton


/**
 * Handles database operations for bookmarked items
 */
@Singleton
class BookmarkItemsDao @Inject constructor(
    @Named("bookmarksItem") private val clientProvider: Provider<ContentProviderClient>
) {

    /**
     * Find all persisted items bookmarks on database
     * @return list of bookmarks
     */
    fun getAllBookmarksItems(): List<DepictedItem> {
        val items = mutableListOf<DepictedItem>()
        val db = clientProvider.get()
        try {
            db.query(
                BookmarkItemsContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                null,
                emptyArray(),
                null
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    items.add(fromCursor(cursor))
                }
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
        return items
    }

    /**
     * Look for a bookmark in database and in order to insert or delete it
     * @param depictedItem : Bookmark object
     * @return boolean : is bookmark now favorite?
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
            db.insert(BookmarkItemsContentProvider.BASE_URI, toContentValues(depictedItem))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    /**
     * Delete a bookmark from database
     * @param depictedItem : Bookmark to delete
     */
    private fun deleteBookmarkItem(depictedItem: DepictedItem) {
        val db = clientProvider.get()
        try {
            db.delete(
                BookmarkItemsContentProvider.uriForName(depictedItem.id),
                null,
                null
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    /**
     * Find a bookmark from database based on its name
     * @param depictedItemID : Bookmark to find
     * @return boolean : is bookmark in database?
     */
    fun findBookmarkItem(depictedItemID: String?): Boolean {
        if (depictedItemID == null) return false // Avoiding NPEs
        val db = clientProvider.get()
        try {
            db.query(
                BookmarkItemsContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                "${Table.COLUMN_ID}=?",
                arrayOf(depictedItemID),
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) return true
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
        return false
    }

    /**
     * Receives real data from cursor
     * @param cursor : Object for storing database data
     * @return DepictedItem
     */
    @SuppressLint("Range")
    fun fromCursor(cursor: Cursor): DepictedItem {
        val fileName = cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME))
        val description = cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION))
        val imageUrl = cursor.getString(cursor.getColumnIndex(Table.COLUMN_IMAGE))
        val instanceListString = cursor.getString(cursor.getColumnIndex(Table.COLUMN_INSTANCE_LIST))
        val instanceList = stringToArray(instanceListString)
        val categoryNameListString =
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_CATEGORIES_NAME_LIST))
        val categoryNameList = stringToArray(categoryNameListString)
        val categoryDescriptionListString =
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_CATEGORIES_DESCRIPTION_LIST))
        val categoryDescriptionList = stringToArray(categoryDescriptionListString)
        val categoryThumbnailListString =
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_CATEGORIES_THUMBNAIL_LIST))
        val categoryThumbnailList = stringToArray(categoryThumbnailListString)
        val categoryList = convertToCategoryItems(
            categoryNameList, categoryDescriptionList, categoryThumbnailList
        )
        val isSelected = cursor.getString(cursor.getColumnIndex(Table.COLUMN_IS_SELECTED)).toBoolean()
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

    private fun convertToCategoryItems(
        categoryNameList: List<String>,
        categoryDescriptionList: List<String>,
        categoryThumbnailList: List<String>
    ): List<CategoryItem> {
        return categoryNameList.mapIndexed { index, name ->
            CategoryItem(
                name,
                categoryDescriptionList.getOrNull(index),
                categoryThumbnailList.getOrNull(index),
                false
            )
        }
    }

    /**
     * Converts string to List
     * @param listString comma separated single string from list items
     * @return List of string
     */
    private fun stringToArray(listString: String): List<String> {
        return listString.split(",")
    }

    /**
     * Converts list to string
     * @param list list of items
     * @return string comma separated single string of items
     */
    private fun arrayToString(list: List<String?>): String {
        return list.joinToString(",")
    }

    /**
     * Takes data from DepictedItem and create a content value object
     * @param depictedItem depicted item
     * @return ContentValues
     */
    private fun toContentValues(depictedItem: DepictedItem): ContentValues {
        val namesOfCommonsCategories = depictedItem.commonsCategories.map { it.name }
        val descriptionsOfCommonsCategories = depictedItem.commonsCategories.map { it.description }
        val thumbnailsOfCommonsCategories = depictedItem.commonsCategories.map { it.thumbnail }

        return ContentValues().apply {
            put(Table.COLUMN_NAME, depictedItem.name)
            put(Table.COLUMN_DESCRIPTION, depictedItem.description)
            put(Table.COLUMN_IMAGE, depictedItem.imageUrl)
            put(Table.COLUMN_INSTANCE_LIST, arrayToString(depictedItem.instanceOfs))
            put(Table.COLUMN_CATEGORIES_NAME_LIST, arrayToString(namesOfCommonsCategories))
            put(
                Table.COLUMN_CATEGORIES_DESCRIPTION_LIST,
                arrayToString(descriptionsOfCommonsCategories)
            )
            put(
                Table.COLUMN_CATEGORIES_THUMBNAIL_LIST,
                arrayToString(thumbnailsOfCommonsCategories)
            )
            put(Table.COLUMN_IS_SELECTED, depictedItem.isSelected.toString())
            put(Table.COLUMN_ID, depictedItem.id)
        }
    }

    /**
     * Table of bookmarksItems data
     */
    object Table {
        const val TABLE_NAME = "bookmarksItems"
        const val COLUMN_NAME = "item_name"
        const val COLUMN_DESCRIPTION = "item_description"
        const val COLUMN_IMAGE = "item_image_url"
        const val COLUMN_INSTANCE_LIST = "item_instance_of"
        const val COLUMN_CATEGORIES_NAME_LIST = "item_name_categories"
        const val COLUMN_CATEGORIES_DESCRIPTION_LIST = "item_description_categories"
        const val COLUMN_CATEGORIES_THUMBNAIL_LIST = "item_thumbnail_categories"
        const val COLUMN_IS_SELECTED = "item_is_selected"
        const val COLUMN_ID = "item_id"

        val ALL_FIELDS = arrayOf(
            COLUMN_NAME,
            COLUMN_DESCRIPTION,
            COLUMN_IMAGE,
            COLUMN_INSTANCE_LIST,
            COLUMN_CATEGORIES_NAME_LIST,
            COLUMN_CATEGORIES_DESCRIPTION_LIST,
            COLUMN_CATEGORIES_THUMBNAIL_LIST,
            COLUMN_IS_SELECTED,
            COLUMN_ID
        )

        private const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"
        private const val CREATE_TABLE_STATEMENT = """
            CREATE TABLE $TABLE_NAME (
                $COLUMN_NAME STRING,
                $COLUMN_DESCRIPTION STRING,
                $COLUMN_IMAGE STRING,
                $COLUMN_INSTANCE_LIST STRING,
                $COLUMN_CATEGORIES_NAME_LIST STRING,
                $COLUMN_CATEGORIES_DESCRIPTION_LIST STRING,
                $COLUMN_CATEGORIES_THUMBNAIL_LIST STRING,
                $COLUMN_IS_SELECTED STRING,
                $COLUMN_ID STRING PRIMARY KEY
            );
        """

        /**
         * Creates table
         * @param db SQLiteDatabase
         */
        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        /**
         * Deletes database
         * @param db SQLiteDatabase
         */
        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        /**
         * Updates database
         * @param db SQLiteDatabase
         * @param from starting version
         * @param to end version
         */
        fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
            if (from == to) return
            if (from < 18) {
                onUpdate(db, from + 1, to)
                return
            }
            if (from == 18) {
                onCreate(db)
                onUpdate(db, from + 1, to)
            }
        }
    }
}
