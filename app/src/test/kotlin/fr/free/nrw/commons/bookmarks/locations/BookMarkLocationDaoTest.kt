package fr.free.nrw.commons.bookmarks.locations

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
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
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class BookMarkLocationDaoTest {

    private lateinit var bookmarkLocationsDao: BookmarkLocationsDao

    private lateinit var database: AppDatabase

    private lateinit var examplePlaceBookmark: Place
    private lateinit var exampleLabel: Label
    private lateinit var exampleLocation: LatLng
    private lateinit var builder: Sitelinks.Builder

    @Before
    fun setUp() {
        exampleLabel = Label.FOREST
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
    fun testForAddAndGetAllBookmarkLocations() = runBlocking {
        bookmarkLocationsDao.addBookmarkLocation(bookmarkLocationsDao.toEntity(examplePlaceBookmark))

        val bookmarks = bookmarkLocationsDao.getAllBookmarksLocations()

        assertEquals(1, bookmarks.size)
        val retrievedBookmark = bookmarks.first()
        assertEquals(examplePlaceBookmark.name, retrievedBookmark.locationName)
        assertEquals(examplePlaceBookmark.language, retrievedBookmark.locationLanguage)
    }

    @Test
    fun testFindBookmarkByNameForTrue() = runBlocking {
        bookmarkLocationsDao.addBookmarkLocation(bookmarkLocationsDao.toEntity(examplePlaceBookmark))

        val exists = bookmarkLocationsDao.findBookmarkLocation(examplePlaceBookmark.name)
        assertTrue(exists)
    }

    @Test
    fun testFindBookmarkByNameForFalse() = runBlocking {
        bookmarkLocationsDao.addBookmarkLocation(bookmarkLocationsDao.toEntity(examplePlaceBookmark))

        val exists = bookmarkLocationsDao.findBookmarkLocation("xyz")
        assertFalse(exists)
    }

    @Test
    fun testDeleteBookmark() = runBlocking {
        val bookmarkLocation = bookmarkLocationsDao.toEntity(examplePlaceBookmark)
        bookmarkLocationsDao.addBookmarkLocation(bookmarkLocation)

        bookmarkLocationsDao.deleteBookmarkLocation(bookmarkLocation)

        val bookmarks = bookmarkLocationsDao.getAllBookmarksLocations()
        assertTrue(bookmarks.isEmpty())
    }

    @Test
    fun testUpdateBookmarkForTrue() = runBlocking {
        val exists = bookmarkLocationsDao.updateBookmarkLocation(examplePlaceBookmark)

        assertTrue(exists)
    }

    @Test
    fun testUpdateBookmarkForFalse() = runBlocking {
        val newBookmark = bookmarkLocationsDao.toEntity(examplePlaceBookmark)
        bookmarkLocationsDao.addBookmarkLocation(newBookmark)

        val exists = bookmarkLocationsDao.updateBookmarkLocation(examplePlaceBookmark)
        assertFalse(exists)
    }

    @Test
    fun testGetAllBookmarksLocationsPlace() = runBlocking {
        val bookmarkLocation = bookmarkLocationsDao.toEntity(examplePlaceBookmark)
        bookmarkLocationsDao.addBookmarkLocation(bookmarkLocation)

        val bookmarks = bookmarkLocationsDao.getAllBookmarksLocationsPlace()
        assertEquals(1, bookmarks.size)
        assertEquals(examplePlaceBookmark.name, bookmarks.first().name)
    }
}
