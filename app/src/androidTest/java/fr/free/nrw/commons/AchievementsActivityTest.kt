package fr.free.nrw.commons

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.profile.ProfileActivity
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class AchievementsActivityTest {

    @get:Rule
    var activityRule = IntentsTestRule(LoginActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
    }

    @After
    fun cleanup() {
        Intents.release()
    }

    @Test
    fun testProfile() {
        UITestHelper.loginUser()
        UITestHelper.skipWelcome()
        onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.scrollTo(), ViewActions.click())
        onView(
            Matchers.allOf(
                withId(R.id.more_profile),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.scroll_view_more_bottom_sheet),
                        0
                    ),
                    0
                )
            )
        ).perform(ViewActions.scrollTo(), ViewActions.click())
        Intents.intended(hasComponent(ProfileActivity::class.java.name))
        UITestHelper.logoutUser()
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }

}
