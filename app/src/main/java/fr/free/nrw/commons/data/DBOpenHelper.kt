package fr.free.nrw.commons.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import fr.free.nrw.commons.category.CategoryDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao
import fr.free.nrw.commons.recentlanguages.RecentLanguagesDao


class DBOpenHelper(
    context: Context
): SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "commons.db"
        private const val DATABASE_VERSION = 20
        const val CONTRIBUTIONS_TABLE = "contributions"
        private const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS %s"
    }

    /**
     * Do not use directly - @Inject an instance where it's needed and let
     * dependency injection take care of managing this as a singleton.
     */
    override fun onCreate(db: SQLiteDatabase) {
        CategoryDao.Table.onCreate(db)
        BookmarkPicturesDao.Table.onCreate(db)
        BookmarkLocationsDao.Table.onCreate(db)
        BookmarkItemsDao.Table.onCreate(db)
        RecentSearchesDao.Table.onCreate(db)
        RecentLanguagesDao.Table.onCreate(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, from: Int, to: Int) {
        CategoryDao.Table.onUpdate(db, from, to)
        BookmarkPicturesDao.Table.onUpdate(db, from, to)
        BookmarkLocationsDao.Table.onUpdate(db, from, to)
        BookmarkItemsDao.Table.onUpdate(db, from, to)
        RecentSearchesDao.Table.onUpdate(db, from, to)
        RecentLanguagesDao.Table.onUpdate(db, from, to)
        deleteTable(db, CONTRIBUTIONS_TABLE)
    }

    /**
     * Delete table in the given db
     * @param db
     * @param tableName
     */
    fun deleteTable(db: SQLiteDatabase, tableName: String) {
        try {
            db.execSQL(String.format(DROP_TABLE_STATEMENT, tableName))
            onCreate(db)
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }
}
