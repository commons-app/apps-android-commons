package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.Intent
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.auth.SignupActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.not
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @get:Rule
    var activityRule = ActivityTestRule(LoginActivity::class.java)

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        Intents.init()
        UITestHelper.skipWelcome()
        intending(not(isInternal())).respondWith(ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun testForgotPassword() {
        Espresso.onView(ViewMatchers.withId(R.id.forgot_password)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(BuildConfig.FORGOT_PASSWORD_URL)
            )
        )
    }

    @Test
    fun testSignupButton() {
        Espresso.onView(ViewMatchers.withId(R.id.sign_up_button)).perform(ViewActions.click())
        Intents.intended(IntentMatchers.hasComponent(SignupActivity::class.java.name))
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}