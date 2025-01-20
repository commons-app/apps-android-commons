package fr.free.nrw.commons.bookmarks.locations

import android.content.ContentProviderClient
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.RemoteException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.inOrder
import com.nhaarman.mockitokotlin2.isA
import com.nhaarman.mockitokotlin2.isNull
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.Sitelinks
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.verifyNoInteractions
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookMarkLocationDaoTest {

    @Mock
    var bookmarkLocationsDao: BookmarkLocationsDao? = null

    private lateinit var database: AppDatabase

    private lateinit var examplePlaceBookmark: Place
    private lateinit var exampleLabel: Label
    private lateinit var exampleUri: Uri
    private lateinit var exampleLocation: LatLng
    private lateinit var builder: Sitelinks.Builder

    @Before
    fun setUp() {
        exampleLabel = Label.FOREST
        exampleUri = Uri.parse("wikimedia/uri")
        exampleLocation = LatLng(40.0, 51.4, 1f)

        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        bookmarkLocationsDao = database.bookmarkLocationsDao()

        builder = Sitelinks.Builder().apply {
            setWikipediaLink("wikipediaLink")
            setWikidataLink("wikidataLink")
            setCommonsLink("commonsLink")
        }

        examplePlaceBookmark =
            Place(
                "en",
                "placeName",
                exampleLabel,
                "placeDescription",
                exampleLocation,
                "placeCategory",
                builder.build(),
                "picName",
                false,
            )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveBookmark() = runBlocking {
        // Insert a bookmark
        bookmarkLocationsDao?.addBookmarkLocation(examplePlaceBookmark.toBookmarksLocations())

        // Retrieve all bookmarks
        val bookmarks = bookmarkLocationsDao?.getAllBookmarksLocations()

        // Assert the bookmark exists
        assertEquals(1, bookmarks?.size)
        val retrievedBookmark = bookmarks?.first()
        assertEquals(examplePlaceBookmark.name, retrievedBookmark?.locationName)
        assertEquals(examplePlaceBookmark.language, retrievedBookmark?.locationLanguage)
    }
}
