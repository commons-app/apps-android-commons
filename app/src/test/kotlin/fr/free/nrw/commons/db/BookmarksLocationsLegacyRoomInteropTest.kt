package fr.free.nrw.commons.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarksLocations
import fr.free.nrw.commons.data.DBOpenHelper
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Ensures bookmark location data written with legacy-style SQL is readable by Room entities (and the
 * reverse), and that [AppDatabaseMigrations.migrationBookmarksFromLegacyCommonsDb] copies rows from
 * the legacy `commons.db` / [DBOpenHelper.BOOKMARKS_LOCATIONS] schema into `bookmarks_locations`.
 *
 * This supports migration work tracked under commons-app/apps-android-commons#6768.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookmarksLocationsLegacyRoomInteropTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        deleteLegacyCommonsDb()
    }

    @After
    fun tearDown() {
        deleteLegacyCommonsDb()
    }

    private fun deleteLegacyCommonsDb() {
        context.getDatabasePath(AppDatabaseMigrations.LEGACY_COMMONS_DATABASE_NAME).let { f ->
            if (f.exists()) {
                f.delete()
            }
        }
        context.getDatabasePath("${AppDatabaseMigrations.LEGACY_COMMONS_DATABASE_NAME}-journal").let { f ->
            if (f.exists()) {
                f.delete()
            }
        }
    }

    /**
     * Uses [Room.inMemoryDatabaseBuilder]: raw SQL inserts into `bookmarks_locations`, then the Room
     * DAO reads the same row.
     */
    @Test
    fun roomDaoReadsRowsInsertedViaLegacySql() =
        runBlocking {
            val database =
                Room
                    .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            try {
                val sqlDb = database.openHelper.writableDatabase
                insertSampleRowViaLegacySql(sqlDb)

                val dao = database.bookmarkLocationsDao()
                val rows = dao.getAllBookmarksLocations()
                assertEquals(1, rows.size)
                assertEquals(sampleEntity(), rows.single())
            } finally {
                database.close()
            }
        }

    /**
     * Room [BookmarksLocations] is persisted via the DAO; the same columns are read back with a raw
     * SQLite query (legacy-style access).
     */
    @Test
    fun legacySqlReadsRowsWrittenByRoomDao() =
        runBlocking {
            val database =
                Room
                    .inMemoryDatabaseBuilder(context, AppDatabase::class.java)
                    .allowMainThreadQueries()
                    .build()
            try {
                val entity = sampleEntity()
                database.bookmarkLocationsDao().addBookmarkLocation(entity)

                val sqlDb = database.openHelper.writableDatabase
                sqlDb
                    .query(
                        SimpleSQLiteQuery(
                            "SELECT * FROM bookmarks_locations WHERE location_name = ?",
                            arrayOf<Any?>(entity.locationName),
                        ),
                    ).use { c ->
                        assertTrue(c.moveToFirst())
                        assertEquals(entity.locationName, c.getString(c.getColumnIndexOrThrow("location_name")))
                        assertEquals(entity.locationLanguage, c.getString(c.getColumnIndexOrThrow("location_language")))
                        assertEquals(entity.locationDescription, c.getString(c.getColumnIndexOrThrow("location_description")))
                        assertEquals(entity.locationLat, c.getDouble(c.getColumnIndexOrThrow("location_lat")), 0.0)
                        assertEquals(entity.locationLong, c.getDouble(c.getColumnIndexOrThrow("location_long")), 0.0)
                        assertEquals(entity.locationCategory, c.getString(c.getColumnIndexOrThrow("location_category")))
                        assertEquals(entity.locationLabelText, c.getString(c.getColumnIndexOrThrow("location_label_text")))
                        assertEquals(
                            entity.locationLabelIcon,
                            if (c.isNull(c.getColumnIndexOrThrow("location_label_icon"))) {
                                null
                            } else {
                                c.getInt(c.getColumnIndexOrThrow("location_label_icon"))
                            },
                        )
                        assertEquals(entity.locationImageUrl, c.getString(c.getColumnIndexOrThrow("location_image_url")))
                        assertEquals(entity.locationWikipediaLink, c.getString(c.getColumnIndexOrThrow("location_wikipedia_link")))
                        assertEquals(entity.locationWikidataLink, c.getString(c.getColumnIndexOrThrow("location_wikidata_link")))
                        assertEquals(entity.locationCommonsLink, c.getString(c.getColumnIndexOrThrow("location_commons_link")))
                        assertEquals(entity.locationPic, c.getString(c.getColumnIndexOrThrow("location_pic")))
                        assertEquals(if (entity.locationExists) 1 else 0, c.getInt(c.getColumnIndexOrThrow("location_exists")))
                    }
            } finally {
                database.close()
            }
        }

    /**
     * Legacy `commons.db` holds [DBOpenHelper.BOOKMARKS_LOCATIONS]; running the 19→20 migration must
     * populate `bookmarks_locations` on the Room database file.
     */
    @Test
    fun migration19To20CopiesLegacyBookmarksTableIntoRoomTable() {
        createLegacyCommonsDbWithSampleRow()

        val roomDbName = "migration_verify_${System.nanoTime()}.db"
        context.getDatabasePath(roomDbName).let { f ->
            if (f.exists()) {
                f.delete()
            }
        }

        val openHelper =
            FrameworkSQLiteOpenHelperFactory()
                .create(
                    SupportSQLiteOpenHelper.Configuration
                        .builder(context)
                        .name(roomDbName)
                        .callback(
                            object : SupportSQLiteOpenHelper.Callback(19) {
                                override fun onCreate(db: SupportSQLiteDatabase) {
                                    // Empty pre-migration Room file: 19→20 adds `bookmarks_locations`.
                                }
                            },
                        ).build(),
                )

        val db = openHelper.writableDatabase
        try {
            AppDatabaseMigrations.migrationBookmarksFromLegacyCommonsDb(context).migrate(db)

            db
                .query(
                    SimpleSQLiteQuery(
                        "SELECT * FROM bookmarks_locations WHERE location_name = ?",
                        arrayOf<Any?>(sampleEntity().locationName),
                    ),
                ).use { c ->
                    assertTrue(c.moveToFirst())
                    assertEquals("en", c.getString(c.getColumnIndexOrThrow("location_language")))
                    assertEquals("A river", c.getString(c.getColumnIndexOrThrow("location_description")))
                    assertEquals(40.0, c.getDouble(c.getColumnIndexOrThrow("location_lat")), 0.0)
                    assertEquals(51.4, c.getDouble(c.getColumnIndexOrThrow("location_long")), 0.0)
                }
        } finally {
            db.close()
            openHelper.close()
            context.getDatabasePath(roomDbName).let { f ->
                if (f.exists()) {
                    f.delete()
                }
            }
        }
    }

    private fun createLegacyCommonsDbWithSampleRow() {
        val path = context.getDatabasePath(AppDatabaseMigrations.LEGACY_COMMONS_DATABASE_NAME).path
        val legacy = SQLiteDatabase.openOrCreateDatabase(path, null)
        legacy.execSQL(
            """
            CREATE TABLE ${DBOpenHelper.BOOKMARKS_LOCATIONS} (
                location_name TEXT NOT NULL,
                location_language TEXT NOT NULL,
                location_description TEXT NOT NULL,
                location_category TEXT NOT NULL,
                location_label_text TEXT NOT NULL,
                location_label_icon INTEGER NOT NULL,
                location_lat REAL NOT NULL,
                location_long REAL NOT NULL,
                location_image_url TEXT,
                location_wikipedia_link TEXT,
                location_wikidata_link TEXT,
                location_commons_link TEXT,
                location_pic TEXT,
                location_exists INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        val e = sampleEntity()
        legacy.execSQL(
            """
            INSERT INTO ${DBOpenHelper.BOOKMARKS_LOCATIONS} (
                location_name, location_language, location_description, location_category,
                location_label_text, location_label_icon, location_lat, location_long,
                location_image_url, location_wikipedia_link, location_wikidata_link,
                location_commons_link, location_pic, location_exists
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                e.locationName,
                e.locationLanguage,
                e.locationDescription,
                e.locationCategory,
                e.locationLabelText,
                e.locationLabelIcon ?: 0,
                e.locationLat,
                e.locationLong,
                e.locationImageUrl,
                e.locationWikipediaLink,
                e.locationWikidataLink,
                e.locationCommonsLink,
                e.locationPic,
                if (e.locationExists) 1 else 0,
            ),
        )
        legacy.close()
    }

    private fun insertSampleRowViaLegacySql(sqlDb: SupportSQLiteDatabase) {
        val e = sampleEntity()
        sqlDb.execSQL(
            """
            INSERT OR REPLACE INTO bookmarks_locations (
                location_name, location_language, location_description, location_category,
                location_label_text, location_label_icon, location_lat, location_long,
                location_image_url, location_wikipedia_link, location_wikidata_link,
                location_commons_link, location_pic, location_exists
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent(),
            arrayOf<Any?>(
                e.locationName,
                e.locationLanguage,
                e.locationDescription,
                e.locationCategory,
                e.locationLabelText,
                e.locationLabelIcon,
                e.locationLat,
                e.locationLong,
                e.locationImageUrl,
                e.locationWikipediaLink,
                e.locationWikidataLink,
                e.locationCommonsLink,
                e.locationPic,
                if (e.locationExists) 1 else 0,
            ),
        )
    }

    private fun assertBookmarksRowMatchesSample(row: BookmarksLocations) {
        val expected = sampleEntity()
        assertEquals(expected.locationName, row.locationName)
        assertEquals(expected.locationLanguage, row.locationLanguage)
        assertEquals(expected.locationDescription, row.locationDescription)
        assertEquals(expected.locationLat, row.locationLat, 0.0)
        assertEquals(expected.locationLong, row.locationLong, 0.0)
        assertEquals(expected.locationCategory, row.locationCategory)
        assertEquals(expected.locationLabelText, row.locationLabelText)
        assertEquals(expected.locationLabelIcon, row.locationLabelIcon)
        assertEquals(expected.locationImageUrl, row.locationImageUrl)
        assertEquals(expected.locationWikipediaLink, row.locationWikipediaLink)
        assertEquals(expected.locationWikidataLink, row.locationWikidataLink)
        assertEquals(expected.locationCommonsLink, row.locationCommonsLink)
        assertEquals(expected.locationPic, row.locationPic)
        assertEquals(expected.locationExists, row.locationExists)
    }

    private fun sampleEntity(): BookmarksLocations =
        BookmarksLocations(
            locationName = "LegacyInteropPlace",
            locationLanguage = "en",
            locationDescription = "A river",
            locationLat = 40.0,
            locationLong = 51.4,
            locationCategory = "waterway",
            locationLabelText = "RIVER",
            locationLabelIcon = null,
            locationImageUrl = "https://example.org/pic.jpg",
            locationWikipediaLink = "https://en.wikipedia.org/wiki/X",
            locationWikidataLink = "https://www.wikidata.org/wiki/Q1",
            locationCommonsLink = "https://commons.wikimedia.org/wiki/File:X.jpg",
            locationPic = "File:X.jpg",
            locationExists = true,
        )
}
