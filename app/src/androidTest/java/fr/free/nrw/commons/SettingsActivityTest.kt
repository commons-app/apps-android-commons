package fr.free.nrw.commons

import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.action.ViewActions.replaceText
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.PreferenceMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import com.google.gson.Gson
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.settings.Prefs
import fr.free.nrw.commons.settings.SettingsActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.core.IsNot.not
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
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
    fun setRecentUploadLimitTo123() {
        // Open "Use external storage" preference
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .perform(click())

        // Try setting it to 123
        Espresso.onView(withId(android.R.id.edit))
                .perform(replaceText("123"))

        // Click "OK"
        Espresso.onView(allOf(withId(android.R.id.button1), withText("OK")))
                .perform(click())

        // Check setting set to 123 in SharedPreferences
        assertEquals(
                123,
                defaultKvStore.getInt(Prefs.UPLOADS_SHOWING, 0).toLong()
        )

        // Check displaying 123 in summary text
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .onChildView(withId(android.R.id.summary))
                .check(matches(withText("123")))
    }

    @Test
    fun setRecentUploadLimitTo0() {
        // Open "Use external storage" preference
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .perform(click())

        // Try setting it to 0
        Espresso.onView(withId(android.R.id.edit))
                .perform(replaceText("0"))

        // Click "OK"
        Espresso.onView(allOf(withId(android.R.id.button1), withText("OK")))
                .perform(click())

        // Check setting set to 100 in SharedPreferences
        assertEquals(
                100,
                defaultKvStore.getInt(Prefs.UPLOADS_SHOWING, 0).toLong()
        )

        // Check displaying 100 in summary text
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .onChildView(withId(android.R.id.summary))
                .check(matches(withText("100")))
    }

    @Test
    fun setRecentUploadLimitTo700() {
        // Open "Use external storage" preference
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .perform(click())

        // Try setting it to 700
        Espresso.onView(withId(android.R.id.edit))
                .perform(replaceText("700"))

        // Click "OK"
        Espresso.onView(allOf(withId(android.R.id.button1), withText("OK")))
                .perform(click())

        // Check setting set to 500 in SharedPreferences
        assertEquals(
                500,
                defaultKvStore.getInt(Prefs.UPLOADS_SHOWING, 0).toLong()
        )

        // Check displaying 100 in summary text
        Espresso.onData(PreferenceMatchers.withKey("uploads"))
                .inAdapterView(withId(android.R.id.list))
                .onChildView(withId(android.R.id.summary))
                .check(matches(withText("500")))
    }

    @Test
    fun useAuthorNameTogglesOn() {
        // Turn on "Use author name" preference if currently off
        if (!defaultKvStore.getBoolean("useAuthorName", false)) {
            Espresso.onData(PreferenceMatchers.withKey("useAuthorName"))
                    .inAdapterView(withId(android.R.id.list))
                    .perform(click())
        }

        // Check authorName preference is enabled
        Espresso.onData(PreferenceMatchers.withKey("authorName"))
                .inAdapterView(withId(android.R.id.list))
                .check(matches(isEnabled()))
    }

    @Test
    fun useAuthorNameTogglesOff() {
        // Turn off "Use external storage" preference if currently on
        if (defaultKvStore.getBoolean("useAuthorName", false)) {
            Espresso.onData(PreferenceMatchers.withKey("useAuthorName"))
                    .inAdapterView(withId(android.R.id.list))
                    .perform(click())
        }

        // Check authorName preference is enabled
        Espresso.onData(PreferenceMatchers.withKey("authorName"))
                .inAdapterView(withId(android.R.id.list))
                .check(matches(not(isEnabled())))
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}
