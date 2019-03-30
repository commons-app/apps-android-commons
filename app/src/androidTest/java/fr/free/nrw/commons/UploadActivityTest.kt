package fr.free.nrw.commons

import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.upload.UploadActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class UploadActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(UploadActivity::class.java)

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}