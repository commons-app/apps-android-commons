package fr.free.nrw.commons

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import fr.free.nrw.commons.explore.categories.ExploreActivity

@RunWith(AndroidJUnit4::class)
class ExploreActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(ExploreActivity::class.java)

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}