package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.contributions.MainActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {
    @get:Rule
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun setup() {
        try {
            Intents.init()
        } catch (ex: IllegalStateException) {

        }
        UITestHelper.skipWelcome()
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    @Test
    fun testLogin() {
        UITestHelper.loginUser()
        Intents.intended(hasComponent(MainActivity::class.java.name))
    }

    @Test
    fun testForgotPassword() {
        Espresso.onView(ViewMatchers.withId(R.id.forgot_password))
                .perform(ViewActions.click())
        Intents.intended(CoreMatchers.allOf(IntentMatchers.hasAction(Intent.ACTION_VIEW), IntentMatchers.hasData(BuildConfig.FORGOT_PASSWORD_URL)));
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}