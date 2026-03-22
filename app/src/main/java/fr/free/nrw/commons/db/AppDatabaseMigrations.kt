package fr.free.nrw.commons.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import fr.free.nrw.commons.data.DBOpenHelper

/**
 * Room [Migration] definitions shared by the app and tests.
 *
 * @see fr.free.nrw.commons.di.CommonsApplicationModule.provideAppDataBase
 */
object AppDatabaseMigrations {

    /**
     * Adds [fr.free.nrw.commons.contributions.Contribution.hasInvalidLocation] to the contribution table.
     */
    val MIGRATION_1_2: Migration =
        object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE contribution " + " ADD COLUMN hasInvalidLocation INTEGER NOT NULL DEFAULT 0",
                )
            }
        }

    /**
     * Copies bookmarked places from the legacy [DBOpenHelper] database ([LEGACY_COMMONS_DATABASE_NAME],
     * table [DBOpenHelper.BOOKMARKS_LOCATIONS]) into Room's `bookmarks_locations` table.
     *
     * @param applicationContext used to resolve the on-disk path of the legacy DB
     */
    fun migrationBookmarksFromLegacyCommonsDb(applicationContext: Context): Migration =
        object : Migration(19, 20) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                CREATE TABLE IF NOT EXISTS bookmarks_locations (
                    location_name TEXT NOT NULL PRIMARY KEY,
                    location_language TEXT NOT NULL,
                    location_description TEXT NOT NULL,
                    location_lat REAL NOT NULL,
                    location_long REAL NOT NULL,
                    location_category TEXT NOT NULL,
                    location_label_text TEXT NOT NULL,
                    location_label_icon INTEGER,
                    location_image_url TEXT NOT NULL DEFAULT '',
                    location_wikipedia_link TEXT NOT NULL,
                    location_wikidata_link TEXT NOT NULL,
                    location_commons_link TEXT NOT NULL,
                    location_pic TEXT NOT NULL,
                    location_exists INTEGER NOT NULL CHECK(location_exists IN (0, 1))
                )
            """,
                )

                val oldDbPath =
                    applicationContext
                        .getDatabasePath(LEGACY_COMMONS_DATABASE_NAME)
                        .path
                val oldDb =
                    SQLiteDatabase.openDatabase(
                        oldDbPath,
                        null,
                        SQLiteDatabase.OPEN_READONLY,
                    )

                val cursor = oldDb.rawQuery("SELECT * FROM ${DBOpenHelper.BOOKMARKS_LOCATIONS}", null)

                while (cursor.moveToNext()) {
                    val locationName =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_name"))
                    val locationLanguage =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_language"))
                    val locationDescription =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_description"))
                    val locationCategory =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_category"))
                    val locationLabelText =
                        cursor.getString(cursor.getColumnIndexOrThrow("location_label_text"))
                    val locationLabelIcon =
                        cursor.getInt(cursor.getColumnIndexOrThrow("location_label_icon"))
                    val locationLat =
                        cursor.getDouble(cursor.getColumnIndexOrThrow("location_lat"))
                    val locationLong =
                        cursor.getDouble(cursor.getColumnIndexOrThrow("location_long"))

                    val locationImageUrl =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_image_url"),
                        ) ?: ""
                    val locationWikipediaLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_wikipedia_link"),
                        ) ?: ""
                    val locationWikidataLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_wikidata_link"),
                        ) ?: ""
                    val locationCommonsLink =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_commons_link"),
                        ) ?: ""
                    val locationPic =
                        cursor.getString(
                            cursor.getColumnIndexOrThrow("location_pic"),
                        ) ?: ""
                    val locationExists =
                        cursor.getInt(
                            cursor.getColumnIndexOrThrow("location_exists"),
                        )

                    db.execSQL(
                        """
                    INSERT OR REPLACE INTO bookmarks_locations (
                        location_name, location_language, location_description, location_category,
                        location_label_text, location_label_icon, location_lat, location_long,
                        location_image_url, location_wikipedia_link, location_wikidata_link,
                        location_commons_link, location_pic, location_exists
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                        arrayOf(
                            locationName,
                            locationLanguage,
                            locationDescription,
                            locationCategory,
                            locationLabelText,
                            locationLabelIcon,
                            locationLat,
                            locationLong,
                            locationImageUrl,
                            locationWikipediaLink,
                            locationWikidataLink,
                            locationCommonsLink,
                            locationPic,
                            locationExists,
                        ),
                    )
                }

                cursor.close()
                oldDb.close()
            }
        }

    /** Same file name as [fr.free.nrw.commons.data.DBOpenHelper] (field is private there). */
    const val LEGACY_COMMONS_DATABASE_NAME: String = "commons.db"
}
