package fr.free.nrw.commons.review

import androidx.room.Room.inMemoryDatabaseBuilder
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.db.AppDatabase
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(AndroidJUnit4::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
@LooperMode(LooperMode.Mode.PAUSED)
class ReviewDaoTest {
    private lateinit var  reviewDao: ReviewDao
    private lateinit var database: AppDatabase

    /**
     * Set up the application database
     */
    @Before
    fun createDb() {
        database = inMemoryDatabaseBuilder(
            context = ApplicationProvider.getApplicationContext(),
            klass = AppDatabase::class.java
        ).allowMainThreadQueries().build()
        reviewDao = database.ReviewDao()
    }

    /**
     * Close the database
     */
    @After
    fun closeDb() {
        database.close()
    }

    /**
     * Test insertion
     * Also checks isReviewedAlready():
     * Case 1: When image has been reviewed/skipped by the user
     */
    @Test
    fun insert() {
        // Insert data
        val imageId = "1234"
        val reviewEntity = ReviewEntity(imageId)
        reviewDao.insert(reviewEntity)

        // Check insertion
        // Covers the case where the image exists in the database
        // And isReviewedAlready() returns true
        val isInserted = reviewDao.isReviewedAlready(imageId)
        MatcherAssert.assertThat(isInserted, CoreMatchers.equalTo(true))
    }

    @Test
    fun isReviewedAlready() {
        /**
         * Test review status of the image
         * Case 2: When image has not been reviewed/skipped
         */
        val imageId = "5856"
        val isInserted = reviewDao.isReviewedAlready(imageId)
        MatcherAssert.assertThat(isInserted, CoreMatchers.equalTo(false))
    }
}