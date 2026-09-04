package fr.free.nrw.commons.db

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.MIGRATION_21_22
import fr.free.nrw.commons.di.MIGRATION_22_23
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MigrationTest {

    @Test
    fun testMigration() {

        val context = ApplicationProvider.getApplicationContext<Context>()

        // legacy "commons.db", mock old data.
        val legacyOpenHelper = DBOpenHelper(context, null)
        val legacyDb = legacyOpenHelper.writableDatabase
        legacyDb.execSQL("INSERT INTO categories (name, description, thumbnail, last_used, times_used) VALUES ('Nature', 'desc', 'thumb', 0, 1)")
        legacyDb.execSQL("INSERT INTO bookmarks (media_name, media_creator) VALUES ('media1', 'creator1')")
        legacyDb.execSQL("INSERT INTO bookmarksItems (item_name, item_description, item_image_url, item_instance_of, item_name_categories, item_description_categories, item_thumbnail_categories, item_is_selected, item_id) VALUES ('item1', 'desc', 'url', 'inst', 'cat', 'cdesc', 'cthumb', 1, 'id1')")
        legacyDb.execSQL("INSERT INTO recent_searches (name, last_used) VALUES ('search1', 123)")
        legacyDb.execSQL("INSERT INTO recent_languages (language_name, language_code) VALUES ('English', 'en')")
        legacyDb.close()

        // mock old version to trigger migration.
        val presentRoomDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "commons_room.db"
        ).allowMainThreadQueries().build()
        presentRoomDatabase.openHelper.writableDatabase.version = 21
        presentRoomDatabase.close()

        val migratingRoomDatabase = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "commons_room.db"
        )
            .addMigrations(MIGRATION_21_22, MIGRATION_22_23)
            .allowMainThreadQueries()
            .build()

        // tests
        try {
            // categories
            var cursor: Cursor = migratingRoomDatabase.query("SELECT * FROM categories", null)
            Assert.assertTrue("category migrated", cursor.moveToFirst())
            Assert.assertEquals("Nature", cursor.getString(cursor.getColumnIndex("name")))
            cursor.close()

            // bookmarks
            cursor = migratingRoomDatabase.query("SELECT * FROM bookmarks", null)
            Assert.assertTrue("bookmark migrated", cursor.moveToFirst())
            Assert.assertEquals("media1", cursor.getString(cursor.getColumnIndex("media_name")))
            cursor.close()

            // bookmark items
            cursor = migratingRoomDatabase.query("SELECT * FROM bookmarksItems", null)
            Assert.assertTrue("bookmarkItems migrated", cursor.moveToFirst())
            Assert.assertEquals("item1", cursor.getString(cursor.getColumnIndex("item_name")))
            cursor.close()

            // recent searches
            cursor = migratingRoomDatabase.query("SELECT * FROM recent_searches", null)
            Assert.assertTrue("recent_searches migrated", cursor.moveToFirst())
            Assert.assertEquals("search1", cursor.getString(cursor.getColumnIndex("name")))
            cursor.close()

            // recent languages
            cursor = migratingRoomDatabase.query("SELECT * FROM recent_languages", null)
            Assert.assertTrue("recent_languages migrated", cursor.moveToFirst())
            Assert.assertEquals("en", cursor.getString(cursor.getColumnIndex("language_code")))
            cursor.close()

            // bookmarks_categories table must exist after 21→22 migration
            cursor = migratingRoomDatabase.query("SELECT * FROM bookmarks_categories WHERE 0=1", null)
            Assert.assertNotNull("bookmarks_categories table exists", cursor)
            cursor.close()

        } finally {
            migratingRoomDatabase.close()
            context.deleteDatabase("commons.db")
            context.deleteDatabase("commons_room.db")
        }
    }

    @Test
    fun testMigration22To23() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val db22 = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "commons_room_22.db"
        ).allowMainThreadQueries().build()
        db22.openHelper.writableDatabase.version = 22
        db22.close()

        val db23 = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "commons_room_22.db"
        )
            .addMigrations(MIGRATION_22_23)
            .allowMainThreadQueries()
            .build()

        try {
            val cursor = db23.query("SELECT * FROM bookmarks_categories WHERE 0=1", null)
            Assert.assertNotNull("bookmarks_categories table exists after 22→23 migration", cursor)
            cursor.close()
        } finally {
            db23.close()
            context.deleteDatabase("commons_room_22.db")
        }
    }
}