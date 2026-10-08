package fr.free.nrw.commons.explore.recentsearches

import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.db.AppDatabase
import fr.free.nrw.commons.explore.models.RecentSearch
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class RecentSearchesDaoTest {
    private lateinit var recentSearchesRoomDao: RecentSearchesRoomDao
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        database =
            inMemoryDatabaseBuilder(
                context = ApplicationProvider.getApplicationContext(),
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        recentSearchesRoomDao = database.recentSearchesRoomDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun saveExistingQuery() {
        // First insert
        val recentSearch = RecentSearch("butterfly", Date(123L))
        recentSearchesRoomDao.save(recentSearch).blockingAwait()

        // Update it
        val updatedSearch = RecentSearch("butterfly", Date(456L))
        recentSearchesRoomDao.save(updatedSearch).blockingAwait()

        // Verify update
        val found = recentSearchesRoomDao.find("butterfly").blockingGet()
        assertNotNull(found)
        assertEquals(456L, found?.lastSearched?.time)
    }

    @Test
    fun saveNewQuery() {
        val recentSearch = RecentSearch("butterfly", Date(123L))
        recentSearchesRoomDao.save(recentSearch).blockingAwait()

        val found = recentSearchesRoomDao.find("butterfly").blockingGet()
        assertNotNull(found)
        assertEquals("butterfly", found?.query)
    }

    @Test
    fun findRecentSearchQuery() {
        val recentSearch = RecentSearch("butterfly", Date(123L))
        recentSearchesRoomDao.save(recentSearch).blockingAwait()

        val found = recentSearchesRoomDao.find("butterfly").blockingGet()
        assertNotNull(found)
        assertEquals("butterfly", found?.query)

        val notFound = recentSearchesRoomDao.find("non existent").blockingGet()
        assertNull(notFound)
    }

    @Test
    fun recentSearchesHonorsLimit() {
        for (i in 1..10) {
            recentSearchesRoomDao.save(RecentSearch("query $i", Date(i * 1000L))).blockingAwait()
        }

        val results = recentSearchesRoomDao.recentSearches(5).blockingGet()
        assertEquals(5, results.size)
        assertEquals("query 10", results[0])
    }
}
