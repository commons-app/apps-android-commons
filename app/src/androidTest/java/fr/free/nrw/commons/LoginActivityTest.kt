package fr.free.nrw.commons

import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SignupActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    @get:Rule
    var activity: ActivityTestRule<*> = ActivityTestRule(LoginActivity::class.java)

    @Test
    fun isSignUpButtonWorks() {
        // Clicks the SignUp Button
        Intents.init()
        Espresso.onView(withId(R.id.signupButton))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())
        intended(hasComponent(SignupActivity::class.java.name))
        Intents.release()
    }

    @Test
    fun isForgotPasswordWorks() {
        // Clicks the forgot password
        Intents.init()
        Espresso.onView(withId(R.id.forgotPassword))
                .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
                .perform(click())
        intended(hasAction(Intent.ACTION_VIEW))
        Intents.release()
    }
}