package fr.free.nrw.commons.explore.recentsearches

import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * This class contains the database table architechture for recent searches, It also contains
 * queries and logic necessary to the create, update, delete this table.
 */
object RecentSearchesTable {
    const val TABLE_NAME: String = "recent_searches"
    const val COLUMN_ID: String = "_id"
    const val COLUMN_NAME: String = "name"
    const val COLUMN_LAST_USED: String = "last_used"

    // NOTE! KEEP IN SAME ORDER AS THEY ARE DEFINED UP THERE. HELPS HARD CODE COLUMN INDICES.
    @JvmField
    val ALL_FIELDS = arrayOf(
        COLUMN_ID,
        COLUMN_NAME,
        COLUMN_LAST_USED,
    )

    const val DROP_TABLE_STATEMENT: String = "DROP TABLE IF EXISTS $TABLE_NAME"

    const val CREATE_TABLE_STATEMENT: String = ("CREATE TABLE $TABLE_NAME ($COLUMN_ID INTEGER PRIMARY KEY,$COLUMN_NAME STRING,$COLUMN_LAST_USED INTEGER);")

    /**
     * This method creates a RecentSearchesTable in SupportSQLiteDatabase
     *
     * @param db SupportSQLiteDatabase
     */
    fun onCreate(db: SupportSQLiteDatabase) = db.execSQL(CREATE_TABLE_STATEMENT)

    /**
     * This method deletes RecentSearchesTable from SupportSQLiteDatabase
     *
     * @param db SupportSQLiteDatabase
     */
    fun onDelete(db: SupportSQLiteDatabase) {
        db.execSQL(DROP_TABLE_STATEMENT)
        onCreate(db)
    }

    /**
     * This method is called on migrating from a older version to a newer version
     *
     * @param db   SupportSQLiteDatabase
     * @param from Version from which we are migrating
     * @param to   Version to which we are migrating
     */
    fun onUpdate(db: SupportSQLiteDatabase, from: Int, to: Int) {
        if (from == to) {
            return
        }
        if (from < 6) {
            // doesn't exist yet
            onUpdate(db, from + 1, to)
            return
        }
        if (from == 6) {
            // table added in version 7
            onCreate(db)
            onUpdate(db, from + 1, to)
            return
        }
        if (from == 7) {
            onUpdate(db, from + 1, to)
            return
        }
    }
}
