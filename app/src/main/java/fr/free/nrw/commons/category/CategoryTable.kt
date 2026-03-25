package fr.free.nrw.commons.category

import androidx.sqlite.db.SupportSQLiteDatabase

object CategoryTable {
    const val TABLE_NAME = "categories"

    const val COLUMN_ID = "_id"
    const val COLUMN_NAME = "name"
    const val COLUMN_DESCRIPTION = "description"
    const val COLUMN_THUMBNAIL = "thumbnail"
    const val COLUMN_LAST_USED = "last_used"
    const val COLUMN_TIMES_USED = "times_used"

    // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
    val ALL_FIELDS = arrayOf(
        COLUMN_ID,
        COLUMN_NAME,
        COLUMN_DESCRIPTION,
        COLUMN_THUMBNAIL,
        COLUMN_LAST_USED,
        COLUMN_TIMES_USED
    )

    const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

    const val CREATE_TABLE_STATEMENT = "CREATE TABLE $TABLE_NAME (" +
            "$COLUMN_ID INTEGER PRIMARY KEY," +
            "$COLUMN_NAME TEXT," +
            "$COLUMN_DESCRIPTION TEXT," +
            "$COLUMN_THUMBNAIL TEXT," +
            "$COLUMN_LAST_USED INTEGER," +
            "$COLUMN_TIMES_USED INTEGER" +
            ");"

    fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(CREATE_TABLE_STATEMENT)

    fun onDelete(db: SupportSQLiteDatabase) {
        db.execSQL(DROP_TABLE_STATEMENT)
        onCreate(db)
    }

    fun onUpdate(db: SupportSQLiteDatabase, from: Int, to: Int) {
        if (from == to) return
        if (from < 4) {
            // doesn't exist yet
            onUpdate(db, from + 1, to)
        } else if (from == 4) {
            // table added in version 5
            onCreate(db)
            onUpdate(db, from + 1, to)
        } else if (from == 5) {
            onUpdate(db, from + 1, to)
        } else if (from == 17) {
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN description TEXT;")
            db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN thumbnail TEXT;")
            onUpdate(db, from + 1, to)
        }
    }
}
