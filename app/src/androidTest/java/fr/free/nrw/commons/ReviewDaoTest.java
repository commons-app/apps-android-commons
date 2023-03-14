package fr.free.nrw.commons;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import fr.free.nrw.commons.db.AppDatabase;
import fr.free.nrw.commons.review.ReviewDao;
import fr.free.nrw.commons.review.ReviewEntity;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ReviewDaoTest {

    private ReviewDao reviewDao;
    private AppDatabase database;

    /**
     * Set up the application database
     */
    @Before
    public void createDb() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(
                context, AppDatabase.class)
            .allowMainThreadQueries()
            .build();
        reviewDao = database.ReviewDao();
    }

    /**
     * Close the database
     */
    @After
    public void closeDb() {
        database.close();
    }

    /**
     * Test insertion
     * Also checks isReviewedAlready():
     * Case 1: When image has been reviewed/skipped by the user
     */
    @Test
    public void insert() {
        // Insert data
        String imageId = "1234";
        ReviewEntity reviewEntity = new ReviewEntity(imageId);
        reviewDao.insert(reviewEntity);

        // Check insertion
        // Covers the case where the image exists in the database
        // And isReviewedAlready() returns true
        Boolean isInserted = reviewDao.isReviewedAlready(imageId);
        assertThat(isInserted, equalTo(true));
    }

    /**
     * Test review status of the image
     * Case 2: When image has not been reviewed/skipped
     */
    @Test
    public void isReviewedAlready(){
        String imageId = "5856";
        Boolean isInserted = reviewDao.isReviewedAlready(imageId);
        assertThat(isInserted, equalTo(false));
    }
}