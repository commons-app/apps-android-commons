package fr.free.nrw.commons.recentlanguages

import android.annotation.SuppressLint
import android.database.sqlite.SQLiteDatabase

object RecentLanguagesTable {
    const val TABLE_NAME = "recent_languages"
    const val COLUMN_NAME = "language_name"
    const val COLUMN_CODE = "language_code"

    // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
    @JvmStatic
    val ALL_FIELDS = arrayOf(
        COLUMN_NAME,
        COLUMN_CODE
    )

    const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

    const val CREATE_TABLE_STATEMENT = "CREATE TABLE $TABLE_NAME (" +
            "$COLUMN_NAME STRING," +
            "$COLUMN_CODE STRING PRIMARY KEY" +
            ");"

    /**
     * This method creates a LanguagesTable in SQLiteDatabase
     * @param db SQLiteDatabase
     */
    @SuppressLint("SQLiteString")
    @JvmStatic
    fun onCreate(db: SQLiteDatabase) {
        db.execSQL(CREATE_TABLE_STATEMENT)
    }

    /**
     * This method deletes LanguagesTable from SQLiteDatabase
     * @param db SQLiteDatabase
     */
    @JvmStatic
    fun onDelete(db: SQLiteDatabase) {
        db.execSQL(DROP_TABLE_STATEMENT)
        onCreate(db)
    }

    /**
     * This method is called on migrating from a older version to a newer version
     * @param db SQLiteDatabase
     * @param from Version from which we are migrating
     * @param to Version to which we are migrating
     */
    @JvmStatic
    fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
        if (from == to) {
            return
        }
        if (from < 19) {
            // doesn't exist yet
            onUpdate(db, from + 1, to)
            return
        }
        if (from == 19) {
            // table added in version 20
            onCreate(db)
            onUpdate(db, from + 1, to)
        }
    }
}
