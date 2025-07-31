package fr.free.nrw.commons.bookmarks.items

import android.database.sqlite.SQLiteDatabase

/**
 * Table of bookmarksItems data
 */
object BookmarkItemsTable {
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

    const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

    val CREATE_TABLE_STATEMENT =
        """CREATE TABLE $TABLE_NAME (
             $COLUMN_NAME STRING,
             $COLUMN_DESCRIPTION STRING,
             $COLUMN_IMAGE STRING,
             $COLUMN_INSTANCE_LIST STRING,
             $COLUMN_CATEGORIES_NAME_LIST STRING,
             $COLUMN_CATEGORIES_DESCRIPTION_LIST STRING,
             $COLUMN_CATEGORIES_THUMBNAIL_LIST STRING,
             $COLUMN_IS_SELECTED STRING,
             $COLUMN_ID STRING PRIMARY KEY
           );""".trimIndent()

    /**
     * Creates table
     *
     * @param db SQLiteDatabase
     */
    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_STATEMENT)
    }

    /**
     * Deletes database
     *
     * @param db SQLiteDatabase
     */
    fun onDelete(db: SQLiteDatabase) {
        db.execSQL(DROP_TABLE_STATEMENT)
        onCreate(db)
    }

    /**
     * Updates database
     *
     * @param db   SQLiteDatabase
     * @param from starting
     * @param to   end
     */
    fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
        if (from == to) {
            return
        }

        if (from < 18) {
            // doesn't exist yet
            onUpdate(db, from + 1, to)
            return
        }

        if (from == 18) {
            // table added in version 19
            onCreate(db)
            onUpdate(db, from + 1, to)
        }
    }
}