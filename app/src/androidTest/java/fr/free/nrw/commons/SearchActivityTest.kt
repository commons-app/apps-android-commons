package fr.free.nrw.commons

import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.rule.ActivityTestRule
import fr.free.nrw.commons.explore.SearchActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SearchActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(SearchActivity::class.java)

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}