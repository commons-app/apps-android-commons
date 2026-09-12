package fr.free.nrw.commons.db

import android.content.Context
import android.database.Cursor
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.data.DBOpenHelper
import fr.free.nrw.commons.di.CommonsApplicationModule.Companion.ALL_MIGRATIONS
import org.junit.Assert
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

private const val ROOM_DB = "commons_room.db"
private const val LEGACY_DB = "commons.db"

/**
 * The version [AppDatabase] currently declares.
 */
private fun latestVersion(context: Context): Int {
    val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
    try {
        return database.openHelper.readableDatabase.version
    } finally {
        database.close()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

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

        // build "commons_room.db" as it was at version 21, then migrate it.
        helper.createDatabase(ROOM_DB, 21).close()
        val migratedDb = helper.runMigrationsAndValidate(
            ROOM_DB, latestVersion(context), true, *ALL_MIGRATIONS
        )

        // tests
        try {
            // categories
            var cursor: Cursor = migratedDb.query("SELECT * FROM categories")
            Assert.assertTrue("category migrated", cursor.moveToFirst())
            Assert.assertEquals("Nature", cursor.getString(cursor.getColumnIndex("name")))
            cursor.close()

            // bookmarks
            cursor = migratedDb.query("SELECT * FROM bookmarks")
            Assert.assertTrue("bookmark migrated", cursor.moveToFirst())
            Assert.assertEquals("media1", cursor.getString(cursor.getColumnIndex("media_name")))
            cursor.close()

            // bookmark items
            cursor = migratedDb.query("SELECT * FROM bookmarksItems")
            Assert.assertTrue("bookmarkItems migrated", cursor.moveToFirst())
            Assert.assertEquals("item1", cursor.getString(cursor.getColumnIndex("item_name")))
            cursor.close()

            // recent searches
            cursor = migratedDb.query("SELECT * FROM recent_searches")
            Assert.assertTrue("recent_searches migrated", cursor.moveToFirst())
            Assert.assertEquals("search1", cursor.getString(cursor.getColumnIndex("name")))
            cursor.close()

            // recent languages
            cursor = migratedDb.query("SELECT * FROM recent_languages")
            Assert.assertTrue("recent_languages migrated", cursor.moveToFirst())
            Assert.assertEquals("en", cursor.getString(cursor.getColumnIndex("language_code")))
            cursor.close()

        } finally {
            migratedDb.close()
            context.deleteDatabase(LEGACY_DB)
            context.deleteDatabase(ROOM_DB)
        }
    }

    @Test
    fun testContributionsSurviveMigration() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        val oldDb = helper.createDatabase(ROOM_DB, 21)
        oldDb.execSQL(
            """
            INSERT INTO contribution (
                pageId, state, transferred, depictedItems, dataLength, hasInvalidLocation, retries,
                mimeType, media_pageId, media_filename, media_captions, media_descriptions,
                media_depictionIds, media_creatorIds, media_categoriesHiddenStatus
            ) VALUES
                (
                    '9000029', 1, 0, '[]', 0, 0, 0,
                    'application/ogg', '9000029', 'File:Big Buck Bunny medium.ogv',
                    '{}', '{}', '[]', '[]', '{}'
                ),
                (
                    '6428847', 1, 0, '[]', 0, 0, 0,
                    NULL, '6428847', 'File:Example.jpg',
                    '{}', '{}', '[]', '[]', '{}'
                )
            """.trimIndent()
        )
        oldDb.close()

        val migratedDb = helper.runMigrationsAndValidate(
            ROOM_DB, latestVersion(context), true, *ALL_MIGRATIONS
        )

        try {
            var cursor: Cursor =
                migratedDb.query("SELECT * FROM contribution WHERE pageId = '9000029'")
            // Asserts a row came back, and moves the cursor onto it for the reads below.
            Assert.assertTrue("contribution migrated", cursor.moveToFirst())
            Assert.assertEquals(
                "File:Big Buck Bunny medium.ogv",
                cursor.getString(cursor.getColumnIndex("media_filename"))
            )
            // application/ogg with a .ogv name, so the extension fallback decides.
            Assert.assertEquals(
                "VIDEO",
                cursor.getString(cursor.getColumnIndex("media_mediaType"))
            )
            cursor.close()

            // Nothing to classify a row with no mime type, so it keeps the default.
            cursor = migratedDb.query("SELECT * FROM contribution WHERE pageId = '6428847'")
            Assert.assertTrue("contribution migrated", cursor.moveToFirst())
            Assert.assertEquals(
                "OTHER",
                cursor.getString(cursor.getColumnIndex("media_mediaType"))
            )
            cursor.close()
        } finally {
            migratedDb.close()
            context.deleteDatabase(ROOM_DB)
        }
    }
}
