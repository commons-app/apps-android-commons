package fr.free.nrw.commons

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.achievements.AchievementsActivity
import fr.free.nrw.commons.auth.LoginActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AchievementsActivityTest {
    @get:Rule
    var activityRule = IntentsTestRule(LoginActivity::class.java)

    @Before
    fun setup() {
        UITestHelper.skipWelcome()
        UITestHelper.loginUser()
    }

    @Test
    fun testAchievements() {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.user_icon)).perform(click())

        Intents.intended(hasComponent(AchievementsActivity::class.java.name))
    }
}
