package fr.free.nrw.commons.category

import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.db.AppDatabase
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode
import java.util.Date

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class CategoryDaoTest {
    private lateinit var categoryRoomDao: CategoryRoomDao
    private lateinit var database: AppDatabase

    @Before
    fun createDb() {
        database =
            inMemoryDatabaseBuilder(
                context = ApplicationProvider.getApplicationContext(),
                klass = AppDatabase::class.java,
            ).allowMainThreadQueries().build()
        categoryRoomDao = database.categoryRoomDao()
    }

    @After
    fun closeDb() {
        database.close()
    }

    @Test
    fun saveExistingCategory() {
        // First insert
        val category = Category("Test Category", "desc", "thumb", Date(1234), 1)
        categoryRoomDao.save(category).blockingAwait()

        val foundBefore = categoryRoomDao.findCategory("Test Category").blockingGet()
        MatcherAssert.assertThat(foundBefore, CoreMatchers.equalTo(true))

        // Update it
        category.timesUsed = 5
        categoryRoomDao.save(category).blockingAwait()

        // Verify update via entity
        val entities = categoryRoomDao.findEntity("Test Category").blockingGet()
        assertEquals(5, entities[0].timesUsed)
    }

    @Test
    fun saveNewCategory() {
        val category = Category("New Category", "desc", "thumb", Date(1234), 1)
        categoryRoomDao.save(category).blockingAwait()

        val items = categoryRoomDao.recentCategories(1).blockingGet()
        assertEquals(1, items.size)
        assertEquals("New Category", items[0].name)
    }

    @Test
    fun findCategory() {
        val category = Category("Category to Find", "desc", "thumb", Date(1234), 1)
        categoryRoomDao.save(category).blockingAwait()

        val isFound = categoryRoomDao.findCategory("Category to Find").blockingGet()
        MatcherAssert.assertThat(isFound, CoreMatchers.equalTo(true))

        val isNotFound = categoryRoomDao.findCategory("Non existent").blockingGet()
        MatcherAssert.assertThat(isNotFound, CoreMatchers.equalTo(false))
    }

    @Test
    fun recentCategoriesHonorsLimit() {
        for (i in 1..10) {
            val category = Category("Category $i", "desc", "thumb", Date(i * 1000L), 1)
            categoryRoomDao.save(category).blockingAwait()
        }

        val items = categoryRoomDao.recentCategories(5).blockingGet()
        assertEquals(5, items.size)
        // Check ordering (LIFO by lastUsed)
        assertEquals("Category 10", items[0].name)
    }
}
