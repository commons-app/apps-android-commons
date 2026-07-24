package fr.free.nrw.commons.recentlanguages

import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.db.AppDatabase
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
class RecentLanguagesDaoUnitTest {
    private lateinit var recentLanguagesRoomDao: RecentLanguagesRoomDao
    private lateinit var database: AppDatabase
    private lateinit var exampleLanguage: Language

    @Before
    fun createDb() {
        database =
            inMemoryDatabaseBuilder(
                context = ApplicationProvider.getApplicationContext(),
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        recentLanguagesRoomDao = database.recentLanguagesRoomDao()
        exampleLanguage = Language("English", "en")
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun testGetRecentLanguages() {
        for (i in 1..5) {
            recentLanguagesRoomDao.addRecentLanguage(Language("Lang$i", "code$i")).blockingAwait()
        }

        val result = recentLanguagesRoomDao.getRecentLanguages().blockingGet()
        assertEquals(5, result.size)
    }

    @Test
    fun findExistingLanguage() {
        recentLanguagesRoomDao.addRecentLanguage(exampleLanguage).blockingAwait()
        assertTrue(recentLanguagesRoomDao.findRecentLanguage(exampleLanguage.languageCode).blockingGet())
    }

    @Test
    fun findNotExistingLanguage() {
        assertFalse(recentLanguagesRoomDao.findRecentLanguage(exampleLanguage.languageCode).blockingGet())
    }

    @Test
    fun testAddNewLanguage() {
        recentLanguagesRoomDao.addRecentLanguage(exampleLanguage).blockingAwait()
        val result = recentLanguagesRoomDao.getRecentLanguages().blockingGet()
        assertEquals(1, result.size)
        assertEquals(exampleLanguage.languageName, result[0].languageName)
    }

    @Test
    fun testDeleteLanguage() {
        recentLanguagesRoomDao.addRecentLanguage(exampleLanguage).blockingAwait()
        assertTrue(recentLanguagesRoomDao.findRecentLanguage(exampleLanguage.languageCode).blockingGet())

        recentLanguagesRoomDao.deleteRecentLanguage(exampleLanguage.languageCode).blockingAwait()
        assertFalse(recentLanguagesRoomDao.findRecentLanguage(exampleLanguage.languageCode).blockingGet())
    }
}
