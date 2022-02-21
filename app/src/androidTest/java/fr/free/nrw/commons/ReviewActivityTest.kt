package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.review.ReviewActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReviewActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(LoginActivity::class.java)

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

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
    fun testReview() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("More"),
                UITestHelper.childAtPosition(
                    UITestHelper.childAtPosition(
                        ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    4
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        Espresso.onView(Matchers.allOf(ViewMatchers.withId(R.id.more_peer_review))).perform(
            ViewActions.scrollTo(),
            ViewActions.click()
        )
        Intents.intended(IntentMatchers.hasComponent(ReviewActivity::class.java.name))
        UITestHelper.sleep(1000)
    }

}