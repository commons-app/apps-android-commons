package fr.free.nrw.commons.bookmarks.locations

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import com.nhaarman.mockitokotlin2.*
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsContentProvider.BASE_URI
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao.Table.*
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookMarkLocationDaoTest {
    private val columns = arrayOf(COLUMN_NAME,
            COLUMN_LANGUAGE,
            COLUMN_DESCRIPTION,
            COLUMN_CATEGORY,
            COLUMN_LABEL_TEXT,
            COLUMN_LABEL_ICON,
            COLUMN_IMAGE_URL,
            COLUMN_WIKIPEDIA_LINK,
            COLUMN_WIKIDATA_LINK,
            COLUMN_COMMONS_LINK,
            COLUMN_LAT,
            COLUMN_LONG,
            COLUMN_PIC,
            COLUMN_EXISTS)
    private val client: ContentProviderClient = mock()
    private val database: SQLiteDatabase = mock()
    private val captor = argumentCaptor<ContentValues>()

    private lateinit var testObject: BookmarkLocationsDao
    private lateinit var examplePlaceBookmark: Place
    private lateinit var exampleLabel: Label
    private lateinit var exampleUri: Uri
    private lateinit var exampleLocation: LatLng
    private lateinit var builder: Sitelinks.Builder

    @Before
    fun setUp() {
        exampleLabel = Label.FOREST
        exampleUri = Uri.parse("wikimedia/uri")
        exampleLocation = LatLng(40.0,51.4, 1f)

        builder = Sitelinks.Builder()
        builder.setWikipediaLink("wikipediaLink")
        builder.setWikidataLink("wikidataLink")
        builder.setCommonsLink("commonsLink")


        examplePlaceBookmark = Place("en", "placeName", exampleLabel, "placeDescription"
                , exampleLocation, "placeCategory", builder.build(),"picName",false)
        testObject = BookmarkLocationsDao { client }
    }

    @Test
    fun createTable() {
        onCreate(database)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun deleteTable() {
        onDelete(database)
        inOrder(database) {
            verify(database).execSQL(DROP_TABLE_STATEMENT)
            verify(database).execSQL(CREATE_TABLE_STATEMENT)
        }
    }

    @Test
    fun createFromCursor() {
        createCursor(1).let { cursor ->
            cursor.moveToFirst()
            testObject.fromCursor(cursor).let {
                assertEquals("en", it.language)
                assertEquals("placeName", it.name)
                assertEquals(Label.FOREST, it.label)
                assertEquals("placeDescription", it.longDescription)
                assertEquals(40.0, it.location.latitude, 0.001)
                assertEquals(51.4, it.location.longitude, 0.001)
                assertEquals("placeCategory", it.category)
                assertEquals(builder.build().wikipediaLink, it.siteLinks.wikipediaLink)
                assertEquals(builder.build().wikidataLink, it.siteLinks.wikidataLink)
                assertEquals(builder.build().commonsLink, it.siteLinks.commonsLink)
                assertEquals("picName",it.pic)
                assertEquals(false, it.exists)
            }
        }
    }

    @Test
    fun getAllLocationBookmarks() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(createCursor(14))

        var result = testObject.allBookmarksLocations

        assertEquals(14,(result.size))

    }

    @Test(expected = RuntimeException::class)
    fun getAllLocationBookmarksTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.allBookmarksLocations
    }

    @Test
    fun getAllLocationBookmarksReturnsEmptyList_emptyCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(createCursor(0))
        assertTrue(testObject.allBookmarksLocations.isEmpty())
    }

    @Test
    fun getAllLocationBookmarksReturnsEmptyList_nullCursor() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(null)
        assertTrue(testObject.allBookmarksLocations.isEmpty())
    }

    @Test
    fun cursorsAreClosedAfterGetAllLocationBookmarksQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.allBookmarksLocations

        verify(mockCursor).close()
    }


    @Test
    fun updateNewLocationBookmark() {
        whenever(client.insert(any(), any())).thenReturn(Uri.EMPTY)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)

        assertTrue(testObject.updateBookmarkLocation(examplePlaceBookmark))
        verify(client).insert(eq(BASE_URI), captor.capture())
        captor.firstValue.let { cv ->
            assertEquals(13, cv.size())
            assertEquals(examplePlaceBookmark.name, cv.getAsString(COLUMN_NAME))
            assertEquals(examplePlaceBookmark.language, cv.getAsString(COLUMN_LANGUAGE))
            assertEquals(examplePlaceBookmark.longDescription, cv.getAsString(COLUMN_DESCRIPTION))
            assertEquals(examplePlaceBookmark.label.text, cv.getAsString(COLUMN_LABEL_TEXT))
            assertEquals(examplePlaceBookmark.category, cv.getAsString(COLUMN_CATEGORY))
            assertEquals(examplePlaceBookmark.location.latitude, cv.getAsDouble(COLUMN_LAT), 0.001)
            assertEquals(examplePlaceBookmark.location.longitude, cv.getAsDouble(COLUMN_LONG), 0.001)
            assertEquals(examplePlaceBookmark.siteLinks.wikipediaLink.toString(), cv.getAsString(COLUMN_WIKIPEDIA_LINK))
            assertEquals(examplePlaceBookmark.siteLinks.wikidataLink.toString(), cv.getAsString(COLUMN_WIKIDATA_LINK))
            assertEquals(examplePlaceBookmark.siteLinks.commonsLink.toString(), cv.getAsString(COLUMN_COMMONS_LINK))
            assertEquals(examplePlaceBookmark.pic.toString(), cv.getAsString(COLUMN_PIC))
            assertEquals(examplePlaceBookmark.exists.toString(), cv.getAsString(COLUMN_EXISTS))
        }
    }

    @Test
    fun updateExistingLocationBookmark() {
        whenever(client.delete(isA(), isNull(), isNull())).thenReturn(1)
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))

        assertFalse(testObject.updateBookmarkLocation(examplePlaceBookmark))
        verify(client).delete(eq(BookmarkLocationsContentProvider.uriForName(examplePlaceBookmark.name)), isNull(), isNull())
    }

    @Test
    fun findExistingLocationBookmark() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(1))
        assertTrue(testObject.findBookmarkLocation(examplePlaceBookmark))
    }

    @Test(expected = RuntimeException::class)
    fun findLocationBookmarkTranslatesExceptions() {
        whenever(client.query(any(), any(), anyOrNull(), any(), anyOrNull())).thenThrow(RemoteException(""))
        testObject.findBookmarkLocation(examplePlaceBookmark)
    }

    @Test
    fun findNotExistingLocationBookmarkReturnsNull_emptyCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(createCursor(0))
        assertFalse(testObject.findBookmarkLocation(examplePlaceBookmark))
    }

    @Test
    fun findNotExistingLocationBookmarkReturnsNull_nullCursor() {
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(null)
        assertFalse(testObject.findBookmarkLocation(examplePlaceBookmark))
    }

    @Test
    fun cursorsAreClosedAfterFindLocationBookmarkQuery() {
        val mockCursor: Cursor = mock()
        whenever(client.query(any(), any(), any(), any(), anyOrNull())).thenReturn(mockCursor)
        whenever(mockCursor.moveToFirst()).thenReturn(false)

        testObject.findBookmarkLocation(examplePlaceBookmark)

        verify(mockCursor).close()
    }

    @Test
    fun migrateTableVersionFrom_v1_to_v2() {
        onUpdate(database, 1, 2)
        // Table didnt exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v2_to_v3() {
        onUpdate(database, 2, 3)
        // Table didnt exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v3_to_v4() {
        onUpdate(database, 3, 4)
        // Table didnt exist before v5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v4_to_v5() {
        onUpdate(database, 4, 5)
        // Table didnt change in version 5
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v5_to_v6() {
        onUpdate(database, 5, 6)
        // Table didnt change in version 6
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v6_to_v7() {
        onUpdate(database, 6, 7)
        // Table didnt change in version 7
        verifyNoInteractions(database)
    }

    @Test
    fun migrateTableVersionFrom_v7_to_v8() {
        onUpdate(database, 7, 8)
        verify(database).execSQL(CREATE_TABLE_STATEMENT)
    }

    @Test
    fun migrateTableVersionFrom_v12_to_v13() {
        onUpdate(database, 12, 13)
        verify(database).execSQL("ALTER TABLE bookmarksLocations ADD COLUMN location_destroyed STRING;")
    }

    @Test
    fun migrateTableVersionFrom_v13_to_v14() {
        onUpdate(database, 13, 14)
        verify(database).execSQL("ALTER TABLE bookmarksLocations ADD COLUMN location_language STRING;")
    }

    @Test
    fun migrateTableVersionFrom_v14_to_v15() {
        onUpdate(database, 14, 15)
        verify(database).execSQL("ALTER TABLE bookmarksLocations ADD COLUMN location_exists STRING;")
    }


    private fun createCursor(rowCount: Int) = MatrixCursor(columns, rowCount).apply {

        for (i in 0 until rowCount) {
            addRow(listOf("placeName", "en", "placeDescription", "placeCategory", exampleLabel.text, exampleLabel.icon,
                    exampleUri, builder.build().wikipediaLink, builder.build().wikidataLink, builder.build().commonsLink,
                    exampleLocation.latitude, exampleLocation.longitude, "picName", "placeExists"))
        }
    }
}
