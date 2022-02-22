package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.action.ViewActions.swipeRight
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.rule.IntentsTestRule
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry.getInstrumentation
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.profile.ProfileActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProfileActivityTest {

    @get:Rule
    var activityRule = IntentsTestRule(LoginActivity::class.java)

    private val device: UiDevice = UiDevice.getInstance(getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        UITestHelper.loginUser()
        UITestHelper.skipWelcome()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @Test
    fun testProfile() {
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
        ).perform(ViewActions.click())
        onView(Matchers.allOf(withId(R.id.more_profile))).perform(
            ViewActions.scrollTo(),
            ViewActions.click()
        )
        swipeRight()
        swipeRight()
        swipeRight()
        swipeRight()
        UITestHelper.sleep(5000)
        Intents.intended(hasComponent(ProfileActivity::class.java.name))
    }

}
