package fr.free.nrw.commons.db

import fr.free.nrw.commons.bookmarks.pictures.BookmarkPictureRoomEntity
import fr.free.nrw.commons.bookmarks.pictures.BookmarksTable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class BookmarkPictureTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTablesExist(BookmarksTable.TABLE_NAME)
        assertTableExists(BookmarksTable.TABLE_NAME, BookmarksTable.ALL_FIELDS)
    }

    @Test
    fun testInsertAndRetrieve() {
        val dao = roomDatabase.bookmarkPicturesRoomDao()
        val bookmark = BookmarkPictureRoomEntity(
            mediaName = "Test Image",
            mediaCreator = "Test Creator"
        )
        dao.insert(bookmark)

        val allBookmarks = dao.getAll().blockingGet()
        assertEquals(1, allBookmarks.size)
        assertEquals("Test Image", allBookmarks[0].mediaName)
        assertEquals("Test Creator", allBookmarks[0].mediaCreator)
        assertRowCount(BookmarksTable.TABLE_NAME, 1)
    }

    @Test
    fun testInsertWithLegacyAndRetrieveWithDao() {
        val db = openHelper.writableDatabase
        val dao = roomDatabase.bookmarkPicturesRoomDao()

        // Insert with legacy SQL
        db.execSQL("INSERT INTO bookmarks (media_name, media_creator) VALUES ('Legacy Image', 'Legacy Creator');")

        val exists: Boolean = dao.findBookmarkByName("Legacy Image").blockingGet()
        assertTrue(exists)
        val allBookmarks = dao.getAll().blockingGet()
        assertEquals(1, allBookmarks.size)
        assertEquals("Legacy Image", allBookmarks[0].mediaName)
        assertRowCount(BookmarksTable.TABLE_NAME, 1)
    }

    @Test
    fun testDeleteBookmark() {
        val dao = roomDatabase.bookmarkPicturesRoomDao()
        val bookmark = BookmarkPictureRoomEntity("Image to delete", "Creator")
        dao.insert(bookmark)
        assertRowCount(BookmarksTable.TABLE_NAME, 1)

        dao.delete(bookmark)
        assertRowCount(BookmarksTable.TABLE_NAME, 0)
    }

    @Test
    fun testClearAllTables() {
        val dao = roomDatabase.bookmarkPicturesRoomDao()
        dao.insert(BookmarkPictureRoomEntity("Image 1", "Creator 1"))
        dao.insert(BookmarkPictureRoomEntity("Image 2", "Creator 2"))
        assertRowCount(BookmarksTable.TABLE_NAME, 2)

        clearAllTables()
        assertRowCount(BookmarksTable.TABLE_NAME, 0)
    }
}