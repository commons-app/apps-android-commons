package fr.free.nrw.commons

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.contributions.MainActivity
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    @Ignore("Fix Failing Test")
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}