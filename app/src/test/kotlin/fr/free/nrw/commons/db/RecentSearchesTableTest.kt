package fr.free.nrw.commons.db

import fr.free.nrw.commons.explore.recentsearches.RecentSearchRoomEntity
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesTable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import org.junit.Test
import java.util.Date

class RecentSearchesTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTablesExist(RecentSearchesTable.TABLE_NAME)
        assertTableExists(RecentSearchesTable.TABLE_NAME, RecentSearchesTable.ALL_FIELDS)
    }

    @Test
    fun testInsertAndRetrieve() {
        val dao = roomDatabase.recentSearchesRoomDao()
        val date = Date()
        val search = RecentSearchRoomEntity(
            query = "Test Search",
            lastSearched = date
        )
        dao.insert(search).blockingGet()

        val retrieved = dao.findEntity("Test Search").blockingGet().firstOrNull()
        assertNotNull(retrieved)
        assertEquals("Test Search", retrieved?.query)
        // Date might lose some precision in DB, but should be close
        assertEquals(date.time, retrieved?.lastSearched?.time)
        assertRowCount(RecentSearchesTable.TABLE_NAME, 1)
    }

    @Test
    fun testInsertWithLegacyAndRetrieveWithDao() {
        val db = openHelper.writableDatabase
        val dao = roomDatabase.recentSearchesRoomDao()

        // Insert with legacy SQL
        val now = System.currentTimeMillis()
        db.execSQL("INSERT INTO recent_searches (name, last_used) VALUES ('Legacy Search', $now);")

        val entity = dao.findEntity("Legacy Search").blockingGet().firstOrNull()
        assertNotNull(entity)
        assertEquals("Legacy Search", entity?.query)
        assertEquals(now, entity?.lastSearched?.time)
        assertRowCount(RecentSearchesTable.TABLE_NAME, 1)
    }

    @Test
    fun testDeleteTable() {
        val dao = roomDatabase.recentSearchesRoomDao()
        dao.insert(RecentSearchRoomEntity(query = "Search 1", lastSearched = Date())).blockingGet()
        dao.insert(RecentSearchRoomEntity(query = "Search 2", lastSearched = Date())).blockingGet()
        assertRowCount(RecentSearchesTable.TABLE_NAME, 2)

        dao.deleteTable().blockingAwait()
        assertRowCount(RecentSearchesTable.TABLE_NAME, 0)
    }

    @Test
    fun testClearAllTables() {
        val dao = roomDatabase.recentSearchesRoomDao()
        dao.insert(RecentSearchRoomEntity(query = "Search 1", lastSearched = Date())).blockingGet()
        assertRowCount(RecentSearchesTable.TABLE_NAME, 1)

        clearAllTables()
        assertRowCount(RecentSearchesTable.TABLE_NAME, 0)
    }
}