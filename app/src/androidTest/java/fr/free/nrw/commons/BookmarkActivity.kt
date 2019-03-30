package fr.free.nrw.commons

import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.auth.LoginActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class BookmarkActivity {
    @get:Rule
    var activityRule = IntentsTestRule(LoginActivity::class.java)

    @Test
    fun orientationChange(){
        UITestHelper.getOrientation(activityRule)
    }
}