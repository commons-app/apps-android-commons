package fr.free.nrw.commons

import android.content.pm.ActivityInfo
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.category.CategoryImagesActivity
import fr.free.nrw.commons.explore.categories.ExploreActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class CategoryImagesActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(CategoryImagesActivity::class.java)

    @Test
    fun orientationChange(){
        UITestHelper.getOrientation(activityRule)
    }
}