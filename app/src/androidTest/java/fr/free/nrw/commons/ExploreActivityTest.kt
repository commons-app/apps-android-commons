package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.auth.LoginActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers.`is`
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExploreActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        UITestHelper.skipLogin()
        Intents.init()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun exploreActivityTest() {
        val actionMenuItemView = onView(
            allOf(
                withId(R.id.action_search), withContentDescription("Search"),
                childAtPosition(
                    childAtPosition(
                        withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        actionMenuItemView.perform(click())

        val searchAutoComplete = onView(
            allOf(
                childAtPosition(
                    allOf(
                        withClassName(`is`("android.widget.LinearLayout")),
                        childAtPosition(
                            withClassName(`is`("android.widget.LinearLayout")),
                            1
                        )
                    ),
                    0
                ),
                isDisplayed()
            )
        )
        searchAutoComplete.perform(replaceText("cat"), closeSoftKeyboard())
        UITestHelper.sleep(5000)
        device.swipe(1000,1400,500,1400,20)
        device.swipe(800,1400,600,1400,20)
        device.swipe(800,1400,600,1400,20)
        device.swipe(800,1400,600,1400,20)
        device.swipe(800,1400,600,1400,20)
        device.swipe(800,1400,600,1400,20)
        UITestHelper.sleep(1000)
    }
}
