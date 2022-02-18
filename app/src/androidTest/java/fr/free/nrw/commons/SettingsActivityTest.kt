package fr.free.nrw.commons

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import com.google.gson.Gson
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.SettingsActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class SettingsActivityTest {
    private lateinit var defaultKvStore: JsonKvStore

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(SettingsActivity::class.java)

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storeName = context.packageName + "_preferences"
        defaultKvStore = JsonKvStore(context, storeName, Gson())
    }

    @Test
    fun useAuthorNameTogglesOn() {
        // Turn on "Use author name" preference if currently off
        if (!defaultKvStore.getBoolean("useAuthorName", false)) {
            Espresso.onView(
                allOf(
                    withId(R.id.recycler_view),
                    childAtPosition(withId(android.R.id.list_container), 0)
                )
            ).perform(
                RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(6, click())
            )
        }
        // Check authorName preference is enabled
        Espresso.onView(
            allOf(
                withId(R.id.recycler_view),
                childAtPosition(withId(android.R.id.list_container), 0)
            )
        ).check(matches(isEnabled()))
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }

    private fun childAtPosition(
        parentMatcher: Matcher<View>, position: Int
    ): Matcher<View?> {
        return object : TypeSafeMatcher<View?>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            override fun matchesSafely(view: View?): Boolean {
                val parent = view?.parent
                return (parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position))
            }
        }
    }

}
