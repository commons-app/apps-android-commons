package fr.free.nrw.commons.db

import fr.free.nrw.commons.recentlanguages.RecentLanguageRoomEntity
import fr.free.nrw.commons.recentlanguages.RecentLanguagesTable
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.junit.Test

class RecentLanguagesTableTest : InMemoryDatabaseTest() {

    @Test
    fun testTableExists() {
        assertTablesExist(RecentLanguagesTable.TABLE_NAME)
        assertTableExists(RecentLanguagesTable.TABLE_NAME, RecentLanguagesTable.ALL_FIELDS)
    }

    @Test
    fun testInsertAndRetrieve() {
        val dao = roomDatabase.recentLanguagesRoomDao()
        val language = RecentLanguageRoomEntity(
            languageName = "English",
            languageCode = "en"
        )
        dao.insert(language)

        val allLanguages = dao.getAll().blockingGet()
        assertEquals(1, allLanguages.size)
        assertEquals("English", allLanguages[0].languageName)
        assertEquals("en", allLanguages[0].languageCode)
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 1)
    }

    @Test
    fun testInsertWithLegacyAndRetrieveWithDao() {
        val db = openHelper.writableDatabase
        val dao = roomDatabase.recentLanguagesRoomDao()

        // Insert with legacy SQL
        db.execSQL("INSERT INTO recent_languages (language_name, language_code) VALUES ('French', 'fr');")

        val exists = dao.findRecentLanguage("fr")
        assertTrue(exists)
        val allLanguages = dao.getAll().blockingGet()
        assertEquals(1, allLanguages.size)
        assertEquals("French", allLanguages[0].languageName)
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 1)
    }

    @Test
    fun testDeleteRecentLanguage() {
        val dao = roomDatabase.recentLanguagesRoomDao()
        dao.insert(RecentLanguageRoomEntity("Spanish", "es"))
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 1)

        dao.deleteRecentLanguage("es")
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 0)
    }

    @Test
    fun testClearAllTables() {
        val dao = roomDatabase.recentLanguagesRoomDao()
        dao.insert(RecentLanguageRoomEntity("Language 1", "l1"))
        dao.insert(RecentLanguageRoomEntity("Language 2", "l2"))
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 2)

        clearAllTables()
        assertRowCount(RecentLanguagesTable.TABLE_NAME, 0)
    }
}