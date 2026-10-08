package fr.free.nrw.commons.db

import fr.free.nrw.commons.bookmarks.items.BookmarkItemsRoomEntity
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsTable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class BookmarkItemsTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTablesExist(BookmarkItemsTable.TABLE_NAME)
        assertTableExists(BookmarkItemsTable.TABLE_NAME, BookmarkItemsTable.ALL_FIELDS)
    }

    @Test
    fun testInsertAndRetrieve() {
        val dao = roomDatabase.bookmarkItemsRoomDao()
        val item = BookmarkItemsRoomEntity(
            name = "Test Item",
            description = "Test Description",
            imageUrl = "http://test.com/image.jpg",
            instanceOfs = "Test Instance",
            categoryNames = "Cat1,Cat2",
            categoryDescriptions = "Desc1,Desc2",
            categoryThumbnails = "Thumb1,Thumb2",
            isSelected = true,
            id = "Q12345"
        )
        dao.insert(item).blockingAwait()

        val allItems = dao.getAll().blockingGet()
        assertEquals(1, allItems.size)
        assertEquals("Test Item", allItems[0].name)
        assertEquals("Q12345", allItems[0].id)
        assertTrue(allItems[0].isSelected)
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 1)
    }

    @Test
    fun testInsertWithLegacyAndRetrieveWithDao() {
        val db = openHelper.writableDatabase
        val dao = roomDatabase.bookmarkItemsRoomDao()

        // Insert with legacy SQL
        db.execSQL("""
            INSERT INTO bookmarksItems (item_name, item_id, item_instance_of, item_name_categories, item_description_categories, item_thumbnail_categories, item_is_selected) 
            VALUES ('Legacy Item', 'Q6789', 'Instance', 'Cat', 'Desc', 'Thumb', 1);
        """.trimIndent())

        val exists = dao.findBookmarkItem("Q6789").blockingGet()
        assertTrue(exists)
        val allItems = dao.getAll().blockingGet()
        assertEquals(1, allItems.size)
        assertEquals("Legacy Item", allItems[0].name)
        assertTrue(allItems[0].isSelected)
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 1)
    }

    @Test
    fun testDelete() {
        val dao = roomDatabase.bookmarkItemsRoomDao()
        val item = BookmarkItemsRoomEntity(
            name = "Item to delete",
            description = null,
            imageUrl = null,
            instanceOfs = "",
            categoryNames = "",
            categoryDescriptions = "",
            categoryThumbnails = "",
            isSelected = false,
            id = "QToDelete"
        )
        dao.insert(item).blockingAwait()
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 1)

        dao.delete(item).blockingAwait()
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 0)
    }

    @Test
    fun testClearAllTables() {
        val dao = roomDatabase.bookmarkItemsRoomDao()
        dao.insert(BookmarkItemsRoomEntity("Item 1", null, null, "", "", "", "", false, "Q1")).blockingAwait()
        dao.insert(BookmarkItemsRoomEntity("Item 2", null, null, "", "", "", "", false, "Q2")).blockingAwait()
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 2)

        clearAllTables()
        assertRowCount(BookmarkItemsTable.TABLE_NAME, 0)
    }
}