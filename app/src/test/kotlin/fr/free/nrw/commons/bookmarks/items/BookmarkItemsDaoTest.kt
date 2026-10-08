package fr.free.nrw.commons.bookmarks.items

import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
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
class BookmarkItemsDaoTest {
    private lateinit var bookmarkItemsRoomDao: BookmarkItemsRoomDao
    private lateinit var database: AppDatabase
    private lateinit var exampleItemBookmark: DepictedItem

    @Before
    fun createDb() {
        database =
            inMemoryDatabaseBuilder(
                context = ApplicationProvider.getApplicationContext(),
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        bookmarkItemsRoomDao = database.bookmarkItemsRoomDao()

        exampleItemBookmark =
            DepictedItem(
                "itemName",
                "itemDescription",
                "itemImageUrl",
                listOf("instance"),
                listOf(
                    CategoryItem(
                        "category name",
                        "category description",
                        "category thumbnail",
                        false,
                    ),
                ),
                false,
                "itemID",
            )
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun getAllItemsBookmarks() {
        for (i in 1..5) {
            val item = exampleItemBookmark.copy(id = "item$i")
            bookmarkItemsRoomDao.updateBookmarkItem(item).blockingGet()
        }

        val result = bookmarkItemsRoomDao.getAllBookmarksItems().blockingGet()
        assertEquals(5, result.size)
    }

    @Test
    fun updateNewItemBookmark() {
        assertTrue(bookmarkItemsRoomDao.updateBookmarkItem(exampleItemBookmark).blockingGet())
        assertTrue(bookmarkItemsRoomDao.findBookmarkItem(exampleItemBookmark.id).blockingGet())
    }

    @Test
    fun updateExistingItemBookmark() {
        // First insert
        bookmarkItemsRoomDao.updateBookmarkItem(exampleItemBookmark).blockingGet()
        assertTrue(bookmarkItemsRoomDao.findBookmarkItem(exampleItemBookmark.id).blockingGet())

        // Second update should remove it (toggle behavior)
        assertFalse(bookmarkItemsRoomDao.updateBookmarkItem(exampleItemBookmark).blockingGet())
        assertFalse(bookmarkItemsRoomDao.findBookmarkItem(exampleItemBookmark.id).blockingGet())
    }

    @Test
    fun findExistingItemBookmark() {
        bookmarkItemsRoomDao.updateBookmarkItem(exampleItemBookmark).blockingGet()
        assertTrue(bookmarkItemsRoomDao.findBookmarkItem(exampleItemBookmark.id).blockingGet())
    }

    @Test
    fun findNotExistingItemBookmark() {
        assertFalse(bookmarkItemsRoomDao.findBookmarkItem(exampleItemBookmark.id).blockingGet())
    }
}
