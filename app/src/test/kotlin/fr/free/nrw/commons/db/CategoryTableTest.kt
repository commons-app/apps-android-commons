package fr.free.nrw.commons.db

import fr.free.nrw.commons.category.CategoryRoomEntity
import fr.free.nrw.commons.category.CategoryTable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue
import org.junit.Test

class CategoryTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTablesExist(CategoryTable.TABLE_NAME)
        assertTableExists(CategoryTable.TABLE_NAME, CategoryTable.ALL_FIELDS)
    }

    @Test
    fun testInsertAndRetrieve() {
        val categoryDao = roomDatabase.categoryRoomDao()
        val category = CategoryRoomEntity(
            name = "Test Category",
            description = "Test Description",
            thumbnail = "http://test.com/thumb.jpg",
            timesUsed = 5
        )
        categoryDao.insert(category).blockingGet()

        val retrieved = categoryDao.findEntity("Test Category").blockingGet().firstOrNull()
        assertNotNull(retrieved)
        assertEquals("Test Category", retrieved?.name)
        assertEquals("Test Description", retrieved?.description)
        assertEquals("http://test.com/thumb.jpg", retrieved?.thumbnail)
        assertEquals(5, retrieved?.timesUsed)
        assertRowCount(CategoryTable.TABLE_NAME, 1)
    }

    @Test
    fun testInsertWithLegacyAndRetrieveWithDao() {
        val db = openHelper.writableDatabase
        val categoryDao = roomDatabase.categoryRoomDao()

        // Insert with legacy SQL
        db.execSQL("INSERT INTO categories (name, description, times_used) VALUES ('Nature', 'Nature category', 10);")

        val entity = categoryDao.findEntity("Nature").blockingGet().firstOrNull()
        assertNotNull(entity)
        assertEquals("Nature", entity?.name)
        assertEquals("Nature category", entity?.description)
        assertEquals(10, entity?.timesUsed)
        assertRowCount(CategoryTable.TABLE_NAME, 1)
    }

    @Test
    fun testUpdateCategory() {
        val categoryDao = roomDatabase.categoryRoomDao()
        val category = CategoryRoomEntity(name = "Original Name", timesUsed = 1)
        categoryDao.insert(category).blockingGet()

        val savedCategory = categoryDao.findEntity("Original Name").blockingGet().firstOrNull()
        val updatedCategory = savedCategory?.copy(timesUsed = 2)
        categoryDao.insert(updatedCategory!!).blockingGet()

        val retrieved = categoryDao.findEntity("Original Name").blockingGet().firstOrNull()
        assertEquals(2, retrieved?.timesUsed)
        assertRowCount(CategoryTable.TABLE_NAME, 1)
    }

    @Test
    fun testClearAllTables() {
        val categoryDao = roomDatabase.categoryRoomDao()
        categoryDao.insert(CategoryRoomEntity(name = "Cat 1")).blockingGet()
        categoryDao.insert(CategoryRoomEntity(name = "Cat 2")).blockingGet()
        assertRowCount(CategoryTable.TABLE_NAME, 2)

        clearAllTables()
        assertRowCount(CategoryTable.TABLE_NAME, 0)
    }

    @Test
    fun testFindCategory() {
        val categoryDao = roomDatabase.categoryRoomDao()
        categoryDao.insert(CategoryRoomEntity(name = "Exist")).blockingGet()

        assertTrue(categoryDao.findCategory("Exist").blockingGet())
        assertTrue(!categoryDao.findCategory("NotExist").blockingGet())
    }
}