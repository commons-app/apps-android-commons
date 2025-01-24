package fr.free.nrw.commons

import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import com.google.gson.Gson
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.SettingsActivity
import org.hamcrest.CoreMatchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {
    private lateinit var defaultKvStore: JsonKvStore

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(SettingsActivity::class.java)

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storeName = context.packageName + "_preferences"
        defaultKvStore = JsonKvStore(context, storeName, Gson())
    }

    @Test
    fun useAuthorNameTogglesOn() {
        // Turn on "Use author name" preference if currently off
        if (!defaultKvStore.getBoolean("useAuthorName", false)) {
            Espresso
                .onView(
                    allOf(
                        withId(R.id.recycler_view),
                        childAtPosition(withId(android.R.id.list_container), 0),
                    ),
                ).perform(
                    RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click()),
                )
        }
        // Check authorName preference is enabled
        Espresso
            .onView(
                allOf(
                    withId(R.id.recycler_view),
                    childAtPosition(withId(android.R.id.list_container), 0),
                ),
            ).check(matches(isEnabled()))
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}
