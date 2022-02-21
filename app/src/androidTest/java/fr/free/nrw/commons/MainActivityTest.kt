package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.rule.GrantPermissionRule
import androidx.test.uiautomator.UiDevice
import com.google.gson.Gson
import fr.free.nrw.commons.UITestHelper.Companion.childAtPosition
import fr.free.nrw.commons.auth.LoginActivity
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.notification.NotificationActivity
import org.hamcrest.CoreMatchers
import org.hamcrest.Matchers
import org.junit.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(LoginActivity::class.java)

    @get:Rule
    var mGrantPermissionRule: GrantPermissionRule = GrantPermissionRule.grant(
        "android.permission.ACCESS_FINE_LOCATION"
    )

    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    private lateinit var defaultKvStore: JsonKvStore

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        UITestHelper.loginUser()
        UITestHelper.skipWelcome()
        Intents.init()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val storeName = context.packageName + "_preferences"
        defaultKvStore = JsonKvStore(context, storeName, Gson())
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun testNearby() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("Nearby"),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.fragmentContainer))
            .check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testExplore() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("Explore"),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    2
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.fragmentContainer))
            .check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testBookmarks() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("Bookmarks"),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    3
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
    }

    @Test
    fun testContributions() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withContentDescription("Contributions"),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                        0
                    ),
                    0
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.fragmentContainer))
            .check(matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun testNotifications() {
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.notifications),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.toolbar),
                        1
                    ),
                    1
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        Espresso.pressBack()
        Intents.intended(IntentMatchers.hasComponent(NotificationActivity::class.java.name))
    }

    @Test
    fun testLimitedConnectionModeToggle() {
        val isEnabled = defaultKvStore
            .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false)
        Espresso.onView(
            Matchers.allOf(
                ViewMatchers.withId(R.id.toggle_limited_connection_mode),
                ViewMatchers.withContentDescription("Limited Connection Mode"),
                childAtPosition(
                    childAtPosition(
                        ViewMatchers.withId(R.id.toolbar),
                        1
                    ),
                    0
                ),
                ViewMatchers.isDisplayed()
            )
        ).perform(ViewActions.click())
        if (isEnabled) {
            Assert.assertFalse(
                defaultKvStore
                    .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false)
            )
        } else {
            Assert.assertTrue(
                defaultKvStore
                    .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED, false)
            )
        }
    }

}