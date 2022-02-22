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
import fr.free.nrw.commons.explore.SearchActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SearchActivityTest {

    @get:Rule
    var activityRule = ActivityTestRule(SearchActivity::class.java)

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        Intents.init()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }

    @Test
    fun exploreActivityTest() {
        val searchAutoComplete = Espresso.onView(
            Matchers.allOf(
                UITestHelper.childAtPosition(
                    Matchers.allOf(
                        ViewMatchers.withClassName(Matchers.`is`("android.widget.LinearLayout")),
                        UITestHelper.childAtPosition(
                            ViewMatchers.withClassName(Matchers.`is`("android.widget.LinearLayout")),
                            1
                        )
                    ),
                    0
                ),
                ViewMatchers.isDisplayed()
            )
        )
        searchAutoComplete.perform(ViewActions.replaceText("cat"), ViewActions.closeSoftKeyboard())
        UITestHelper.sleep(5000)
        device.swipe(1000, 1400, 500, 1400, 20)
        device.swipe(800, 1400, 600, 1400, 20)
        device.swipe(800, 1400, 600, 1400, 20)
        device.swipe(800, 1400, 600, 1400, 20)
        device.swipe(800, 1400, 600, 1400, 20)
        device.swipe(800, 1400, 600, 1400, 20)
        UITestHelper.sleep(1000)
    }
}