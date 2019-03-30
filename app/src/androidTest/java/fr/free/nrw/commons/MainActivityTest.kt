package fr.free.nrw.commons

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.contributions.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import android.content.pm.ActivityInfo


@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(MainActivity::class.java)

    @Test
    fun orientationChange(){
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
        activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)

    }
}