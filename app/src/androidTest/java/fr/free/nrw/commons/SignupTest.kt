package fr.free.nrw.commons

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SignupActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class SignupTest {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setup() {
        UITestHelper.skipWelcome()
    }

    @Test
    fun testSignupButton() {
        try {
            Intents.init()
        } catch (ex: IllegalStateException) {

        }

        Espresso.onView(withId(R.id.sign_up_button))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())
        intended(hasComponent(SignupActivity::class.java.name))
        Intents.release()
    }
}