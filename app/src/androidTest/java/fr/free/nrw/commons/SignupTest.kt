package fr.free.nrw.commons

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SignupActivity
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SignupTest {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setup() {
        Intents.init()
        UITestHelper.skipWelcome()
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun testSignupButton() {
        Espresso.onView(withId(R.id.sign_up_button)).perform(click())
        intended(hasComponent(SignupActivity::class.java.name))
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}