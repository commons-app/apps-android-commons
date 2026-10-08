package fr.free.nrw.commons.db

import fr.free.nrw.commons.bookmarks.category.BookmarksCategoryModal
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Test

class BookmarkCategoryTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTableExists("bookmarks_categories", arrayOf("categoryName"))
    }

    @Test
    fun testInsertAndRetrieve() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        val category = BookmarksCategoryModal(categoryName = "Nature")
        dao.insert(category)

        val all = dao.getAllCategories().first()
        assertEquals(1, all.size)
        assertEquals("Nature", all[0].categoryName)
        assertRowCount("bookmarks_categories", 1)
    }

    @Test
    fun testDoesExist() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        assertFalse(dao.doesExist("Animals"))

        dao.insert(BookmarksCategoryModal(categoryName = "Animals"))
        assertTrue(dao.doesExist("Animals"))
    }

    @Test
    fun testDelete() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        val category = BookmarksCategoryModal(categoryName = "Transport")
        dao.insert(category)
        assertRowCount("bookmarks_categories", 1)

        dao.delete(category)
        assertRowCount("bookmarks_categories", 0)
    }

    @Test
    fun testInsertMultipleAndRetrieveAll() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        dao.insert(BookmarksCategoryModal("Architecture"))
        dao.insert(BookmarksCategoryModal("People"))
        dao.insert(BookmarksCategoryModal("Science"))

        val all = dao.getAllCategories().first()
        assertEquals(3, all.size)
        assertRowCount("bookmarks_categories", 3)
    }

    @Test
    fun testInsertDuplicateReplacesExisting() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        dao.insert(BookmarksCategoryModal("Food"))
        dao.insert(BookmarksCategoryModal("Food"))

        assertRowCount("bookmarks_categories", 1)
    }

    @Test
    fun testClearAllTables() = runBlocking {
        val dao = roomDatabase.bookmarkCategoriesDao()
        dao.insert(BookmarksCategoryModal("Music"))
        dao.insert(BookmarksCategoryModal("Sport"))
        assertRowCount("bookmarks_categories", 2)

        clearAllTables()
        assertRowCount("bookmarks_categories", 0)
    }
}
