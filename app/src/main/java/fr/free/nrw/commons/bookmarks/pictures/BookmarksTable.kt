package fr.free.nrw.commons.bookmarks.pictures

import android.database.sqlite.SQLiteDatabase

object BookmarksTable {
    const val TABLE_NAME: String = "bookmarks"
    const val COLUMN_MEDIA_NAME: String = "media_name"
    const val COLUMN_CREATOR: String = "media_creator"

    // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
    val ALL_FIELDS = arrayOf(
        COLUMN_MEDIA_NAME,
        COLUMN_CREATOR
    )

    const val DROP_TABLE_STATEMENT: String = "DROP TABLE IF EXISTS $TABLE_NAME"

    const val CREATE_TABLE_STATEMENT: String = ("CREATE TABLE $TABLE_NAME (" +
            "$COLUMN_MEDIA_NAME STRING PRIMARY KEY, " +
            "$COLUMN_CREATOR STRING" +
            ");")

    fun onCreate(db: SQLiteDatabase) =
        db.execSQL(CREATE_TABLE_STATEMENT)

    fun onDelete(db: SQLiteDatabase) {
        db.execSQL(DROP_TABLE_STATEMENT)
        onCreate(db)
    }

    fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
        if (from == to) {
            return
        }

        if (from < 7) {
            // doesn't exist yet
            onUpdate(db, from+1, to)
            return
        }

        if (from == 7) {
            // table added in version 8
            onCreate(db)
            onUpdate(db, from+1, to)
            return
        }

        if (from == 8) {
            onUpdate(db, from+1, to)
            return
        }
    }
}
