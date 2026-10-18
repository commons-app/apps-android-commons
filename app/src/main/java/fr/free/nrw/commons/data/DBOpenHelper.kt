package fr.free.nrw.commons.data

import android.content.Context
import android.database.sqlite.SQLiteException
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable
import fr.free.nrw.commons.category.CategoryTable
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable


class DBOpenHelper(
    context: Context,
    testDelegate: SupportSQLiteOpenHelper? = null
) {

    companion object {
        private const val DATABASE_NAME = "commons.db"
        private const val DATABASE_VERSION = 22
        const val CONTRIBUTIONS_TABLE = "contributions"
        const val BOOKMARKS_LOCATIONS = "bookmarksLocations"
        private const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS %s"
    }

    /**
     * Do not use directly - @Inject an instance where it's needed and let
     * dependency injection take care of managing this as a singleton.
     */
    private fun createAllTables(db: SupportSQLiteDatabase) {
        CategoryTable.onCreate(db)
        BookmarksTable.onCreate(db)
        BookmarkItemsTable.onCreate(db)
        RecentSearchesTable.onCreate(db)
        RecentLanguagesTable.onCreate(db)
    }

    /**
     *  Delegate to handle database lifecycle
     *  Used to expose writableDatabase , readableDatabase properties
     */
    private val delegate: SupportSQLiteOpenHelper =
        testDelegate ?: FrameworkSQLiteOpenHelperFactory().create(
        SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(DATABASE_NAME)
            .callback(object : SupportSQLiteOpenHelper.Callback(DATABASE_VERSION) {

                override fun onCreate(db: SupportSQLiteDatabase) {
                    createAllTables(db)
                }

                override fun onUpgrade(
                    db: SupportSQLiteDatabase,
                    oldVersion: Int,
                    newVersion: Int
                ) {
                    CategoryTable.onUpdate(db, oldVersion, newVersion)
                    BookmarksTable.onUpdate(db, oldVersion, newVersion)
                    BookmarkItemsTable.onUpdate(db, oldVersion, newVersion)
                    RecentSearchesTable.onUpdate(db, oldVersion, newVersion)
                    RecentLanguagesTable.onUpdate(db, oldVersion, newVersion)
                    deleteTable(db, CONTRIBUTIONS_TABLE)
                    deleteTable(db, BOOKMARKS_LOCATIONS)
                }

            })
            .build()
    )

    val writableDatabase: SupportSQLiteDatabase
        get() = delegate.writableDatabase

    val readableDatabase: SupportSQLiteDatabase
        get() = delegate.readableDatabase


    /**
     * Delete table in the given db
     * @param db
     * @param tableName
     */
    fun deleteTable(db: SupportSQLiteDatabase, tableName: String) {
        try {
            db.execSQL(String.format(DROP_TABLE_STATEMENT, tableName))
            createAllTables(db)
        } catch (e: SQLiteException) {
            e.printStackTrace()
        }
    }
}
