package fr.free.nrw.commons.bookmarks.locations


import android.annotation.SuppressLint
import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.os.RemoteException
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Provider


class BookmarkLocationsDao @Inject constructor(
    @Named("bookmarksLocation") private val clientProvider: Provider<ContentProviderClient>
) {

    /**
     * Find all persisted location bookmarks in the database
     * @return list of Place
     */
    fun getAllBookmarksLocations(): List<Place> {
        val items = mutableListOf<Place>()
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkLocationsContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                null,
                arrayOf(),
                null
            )
            while (cursor?.moveToNext() == true) {
                items.add(fromCursor(cursor))
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.close()
        }
        return items
    }

    /**
     * Look for a place in bookmarks table to insert or delete it
     * @param bookmarkLocation: Place object
     * @return is Place now a favorite?
     */
    fun updateBookmarkLocation(bookmarkLocation: Place): Boolean {
        val bookmarkExists = findBookmarkLocation(bookmarkLocation)
        if (bookmarkExists) {
            deleteBookmarkLocation(bookmarkLocation)
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, false)
        } else {
            addBookmarkLocation(bookmarkLocation)
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, true)
        }
        return !bookmarkExists
    }

    /**
     * Add a Place to bookmarks table
     * @param bookmarkLocation: Place to add
     */
    private fun addBookmarkLocation(bookmarkLocation: Place) {
        val db = clientProvider.get()
        try {
            db.insert(BookmarkLocationsContentProvider.BASE_URI, toContentValues(bookmarkLocation))
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    /**
     * Delete a Place from bookmarks table
     * @param bookmarkLocation: Place to delete
     */
    private fun deleteBookmarkLocation(bookmarkLocation: Place) {
        val db = clientProvider.get()
        try {
            db.delete(
                BookmarkLocationsContentProvider.uriForName(bookmarkLocation.name),
                null,
                null
            )
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            db.close()
        }
    }

    /**
     * Find a Place from database based on its name
     * @param bookmarkLocation: Place to find
     * @return is Place in the database?
     */
    fun findBookmarkLocation(bookmarkLocation: Place): Boolean {
        var cursor: Cursor? = null
        val db = clientProvider.get()
        try {
            cursor = db.query(
                BookmarkLocationsContentProvider.BASE_URI,
                Table.ALL_FIELDS,
                "${Table.COLUMN_NAME}=?",
                arrayOf(bookmarkLocation.name),
                null
            )
            if (cursor?.moveToFirst() == true) {
                return true
            }
        } catch (e: RemoteException) {
            throw RuntimeException(e)
        } finally {
            cursor?.close()
            db.close()
        }
        return false
    }

    @SuppressLint("Range")
    private fun fromCursor(cursor: Cursor): Place {
        val location = LatLng(
            cursor.getDouble(cursor.getColumnIndex(Table.COLUMN_LAT)),
            cursor.getDouble(cursor.getColumnIndex(Table.COLUMN_LONG)),
            1f
        )

        val builder = Sitelinks.Builder().apply {
            setWikipediaLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_WIKIPEDIA_LINK)))
            setWikidataLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_WIKIDATA_LINK)))
            setCommonsLink(cursor.getString(cursor.getColumnIndex(Table.COLUMN_COMMONS_LINK)))
        }

        return Place(
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_LANGUAGE)),
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_NAME)),
            Label.fromText(cursor.getString(cursor.getColumnIndex(Table.COLUMN_LABEL_TEXT))),
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_DESCRIPTION)),
            location,
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_CATEGORY)),
            builder.build(),
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_PIC)),
            cursor.getString(cursor.getColumnIndex(Table.COLUMN_EXISTS)).toBoolean()
        )
    }

    private fun toContentValues(bookmarkLocation: Place): ContentValues {
        return ContentValues().apply {
            put(Table.COLUMN_NAME, bookmarkLocation.name)
            put(Table.COLUMN_LANGUAGE, bookmarkLocation.language)
            put(Table.COLUMN_DESCRIPTION, bookmarkLocation.longDescription)
            put(Table.COLUMN_CATEGORY, bookmarkLocation.category)
            put(Table.COLUMN_LABEL_TEXT, bookmarkLocation.label?.text ?: "")
            put(Table.COLUMN_LABEL_ICON, bookmarkLocation.label?.icon)
            put(Table.COLUMN_WIKIPEDIA_LINK, bookmarkLocation.siteLinks.wikipediaLink.toString())
            put(Table.COLUMN_WIKIDATA_LINK, bookmarkLocation.siteLinks.wikidataLink.toString())
            put(Table.COLUMN_COMMONS_LINK, bookmarkLocation.siteLinks.commonsLink.toString())
            put(Table.COLUMN_LAT, bookmarkLocation.location.latitude)
            put(Table.COLUMN_LONG, bookmarkLocation.location.longitude)
            put(Table.COLUMN_PIC, bookmarkLocation.pic)
            put(Table.COLUMN_EXISTS, bookmarkLocation.exists.toString())
        }
    }

    object Table {
        const val TABLE_NAME = "bookmarksLocations"
        const val COLUMN_NAME = "location_name"
        const val COLUMN_LANGUAGE = "location_language"
        const val COLUMN_DESCRIPTION = "location_description"
        const val COLUMN_LAT = "location_lat"
        const val COLUMN_LONG = "location_long"
        const val COLUMN_CATEGORY = "location_category"
        const val COLUMN_LABEL_TEXT = "location_label_text"
        const val COLUMN_LABEL_ICON = "location_label_icon"
        const val COLUMN_IMAGE_URL = "location_image_url"
        const val COLUMN_WIKIPEDIA_LINK = "location_wikipedia_link"
        const val COLUMN_WIKIDATA_LINK = "location_wikidata_link"
        const val COLUMN_COMMONS_LINK = "location_commons_link"
        const val COLUMN_PIC = "location_pic"
        const val COLUMN_EXISTS = "location_exists"

        val ALL_FIELDS = arrayOf(
            COLUMN_NAME,
            COLUMN_LANGUAGE,
            COLUMN_DESCRIPTION,
            COLUMN_CATEGORY,
            COLUMN_LABEL_TEXT,
            COLUMN_LABEL_ICON,
            COLUMN_LAT,
            COLUMN_LONG,
            COLUMN_IMAGE_URL,
            COLUMN_WIKIPEDIA_LINK,
            COLUMN_WIKIDATA_LINK,
            COLUMN_COMMONS_LINK,
            COLUMN_PIC,
            COLUMN_EXISTS
        )

        private const val DROP_TABLE_STATEMENT = "DROP TABLE IF EXISTS $TABLE_NAME"

        private const val CREATE_TABLE_STATEMENT = """
        CREATE TABLE $TABLE_NAME (
            $COLUMN_NAME STRING PRIMARY KEY,
            $COLUMN_LANGUAGE STRING,
            $COLUMN_DESCRIPTION STRING,
            $COLUMN_CATEGORY STRING,
            $COLUMN_LABEL_TEXT STRING,
            $COLUMN_LABEL_ICON INTEGER,
            $COLUMN_LAT DOUBLE,
            $COLUMN_LONG DOUBLE,
            $COLUMN_IMAGE_URL STRING,
            $COLUMN_WIKIPEDIA_LINK STRING,
            $COLUMN_WIKIDATA_LINK STRING,
            $COLUMN_COMMONS_LINK STRING,
            $COLUMN_PIC STRING,
            $COLUMN_EXISTS STRING
        )
    """

        fun onCreate(db: SQLiteDatabase) {
            db.execSQL(CREATE_TABLE_STATEMENT)
        }

        fun onDelete(db: SQLiteDatabase) {
            db.execSQL(DROP_TABLE_STATEMENT)
            onCreate(db)
        }

        @SuppressLint("SQLiteString")
        fun onUpdate(db: SQLiteDatabase, from: Int, to: Int) {
            Timber.d("bookmarksLocations db is updated from: $from, to: $to")
            if (from == to) return

            if (from < 7) {
                // doesn't exist yet
                onUpdate(db, from + 1, to)
                return
            }

            if (from == 7) {
                // table added in version 8
                onCreate(db)
                onUpdate(db, from + 1, to)
                return
            }

            if (from < 10) {
                onUpdate(db, from + 1, to)
                return
            }

            if (from == 10) {
                // Adding column `location_pic`
                try {
                    db.execSQL(
                        "ALTER TABLE $TABLE_NAME ADD COLUMN location_pic STRING;"
                    )
                } catch (exception: SQLiteException) {
                    Timber.e(exception)
                }
                return
            }

            if (from >= 12) {
                try {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN location_destroyed STRING;")
                } catch (exception: SQLiteException) {
                    Timber.e(exception)
                }
            }

            if (from >= 13) {
                try {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN location_language STRING;")
                } catch (exception: SQLiteException) {
                    Timber.e(exception)
                }
            }

            if (from >= 14) {
                try {
                    db.execSQL("ALTER TABLE $TABLE_NAME ADD COLUMN location_exists STRING;")
                } catch (exception: SQLiteException) {
                    Timber.e(exception)
                }
            }
        }
    }
}