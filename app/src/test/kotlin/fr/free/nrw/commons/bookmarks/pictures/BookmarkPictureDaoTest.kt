package fr.free.nrw.commons.bookmarks.pictures

import android.net.Uri
import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.db.AppDatabase
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class BookmarkPictureDaoTest {
    private lateinit var bookmarkPicturesRoomDao: BookmarkPicturesRoomDao
    private lateinit var database: AppDatabase
    private lateinit var exampleBookmark: Bookmark

    @Before
    fun createDb() {
        database =
            inMemoryDatabaseBuilder(
                context = ApplicationProvider.getApplicationContext(),
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        bookmarkPicturesRoomDao = database.bookmarkPicturesRoomDao()
        exampleBookmark = Bookmark("mediaName", "creatorName")
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getAllBookmarks() {
        for (i in 1..5) {
            bookmarkPicturesRoomDao.updateBookmark(Bookmark("media $i", "creator")).blockingGet()
        }

        val result = bookmarkPicturesRoomDao.getAllBookmarks().blockingGet()
        assertEquals(5, result.size)
    }

    @Test
    fun updateNewBookmark() {
        assertTrue(bookmarkPicturesRoomDao.updateBookmark(exampleBookmark).blockingGet())
        assertTrue(bookmarkPicturesRoomDao.findBookmark(exampleBookmark).blockingGet())
    }

    @Test
    fun updateExistingBookmark() {
        // First insert
        bookmarkPicturesRoomDao.updateBookmark(exampleBookmark).blockingGet()
        assertTrue(bookmarkPicturesRoomDao.findBookmark(exampleBookmark).blockingGet())

        // Second update should remove it (matches legacy behavior)
        assertFalse(bookmarkPicturesRoomDao.updateBookmark(exampleBookmark).blockingGet())
        assertFalse(bookmarkPicturesRoomDao.findBookmark(exampleBookmark).blockingGet())
    }

    @Test
    fun findExistingBookmark() {
        bookmarkPicturesRoomDao.updateBookmark(exampleBookmark).blockingGet()
        assertTrue(bookmarkPicturesRoomDao.findBookmark(exampleBookmark).blockingGet())
    }

    @Test
    fun findNotExistingBookmark() {
        assertFalse(bookmarkPicturesRoomDao.findBookmark(exampleBookmark).blockingGet())
    }
}
