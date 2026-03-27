package fr.free.nrw.commons.db

import android.database.Cursor
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable
import fr.free.nrw.commons.category.CategoryTable
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable
import junit.framework.TestCase
import org.junit.After
import org.junit.Assert
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
abstract class InMemoryDatabaseTest {
    val roomDatabase: AppDatabase by lazy {
        Room.inMemoryDatabaseBuilder(
            context = ApplicationProvider.getApplicationContext(),
            klass = AppDatabase::class.java,
        )
            .allowMainThreadQueries()
            .addCallback(object : RoomDatabase.Callback() {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    super.onCreate(db)
                    // legacy table creation
                    CategoryTable.onDelete(db)
                    BookmarksTable.onDelete(db)
                    BookmarkItemsTable.onDelete(db)
                    RecentSearchesTable.onDelete(db)
                    RecentLanguagesTable.onDelete(db)
                }
            })
            .build()
    }

    val openHelper: DBOpenHelper by lazy {
        DBOpenHelper(ApplicationProvider.getApplicationContext(), roomDatabase.openHelper)
    }

    @After
    fun cleanup() {
        roomDatabase.close()
    }

    fun clearAllTables() = roomDatabase.clearAllTables()

    // Just test that there is a table with the correct name
    fun assertTablesExist(vararg tableNames: String) {
        tableNames.forEach(::assertTableExists)
    }

    // Test that the given table exists, and if you give field names, verify they exist in
    // that table.
    fun assertTableExists(tableName: String, expectedFields: Array<String> = emptyArray()) {
        var cursor: Cursor? = null
        try {
            cursor = openHelper.writableDatabase.query(
                "select * from $tableName where 0=1"
            )

            if (expectedFields.isNotEmpty()) {
                Assert.assertEquals(
                    "Wrong column count for \"$tableName\"",
                    expectedFields.size, cursor.columnCount
                )
                expectedFields.forEach {
                    // Unknown fields have a negative index
                    TestCase.assertTrue(
                        "Field \"$it\" not found in \"$tableName\"",
                        cursor.getColumnIndex(it) >= 0
                    )
                }
            }
        } finally {
            cursor?.close()
        }
    }

    fun assertRowCount(tableName: String, expectedCount: Int) {
        var cursor: Cursor? = null
        try {
            cursor = openHelper.writableDatabase.query(
                "select count(*) from $tableName"
            )
            TestCase.assertTrue(cursor.moveToFirst())
            val actualCount = cursor.getInt(0)
            Assert.assertEquals("Wrong row count for \"$tableName\"", expectedCount, actualCount)
        } finally {
            cursor?.close()
        }
    }
}