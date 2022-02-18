package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.view.View
import androidx.test.espresso.Espresso
import androidx.test.espresso.PerformException
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.isInternal
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.rule.ActivityTestRule
import com.google.android.material.tabs.TabLayout
import fr.free.nrw.commons.auth.LoginActivity
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LeaderboardActivityTest {
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
    @Ignore("Fix Failing Test")
    fun testScrollToRankFromAbove() {
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())

        Espresso.onView(ViewMatchers.withId(R.id.tab_layout)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.tab_layout)).perform(selectTabAtPosition(1))

        UITestHelper.sleep(10000)

        Espresso.onView(ViewMatchers.withId(R.id.scroll)).perform(ViewActions.click())
    }

    @Test
    @Ignore("Fix Failing Test")
    fun testScrollToRankFromBelow() {
        Espresso.onView(ViewMatchers.withId(R.id.drawer_layout)).perform(DrawerActions.open())

        Espresso.onView(ViewMatchers.withId(R.id.tab_layout)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(R.id.tab_layout)).perform(selectTabAtPosition(1))

        UITestHelper.sleep(10000)

        Espresso.onView(ViewMatchers.withId(R.id.leaderboard_list)).perform(ViewActions.swipeUp())
        Espresso.onView(ViewMatchers.withId(R.id.leaderboard_list)).perform(ViewActions.swipeUp())

        Espresso.onView(ViewMatchers.withId(R.id.scroll)).perform(ViewActions.click())
    }

    private fun selectTabAtPosition(tabIndex: Int): ViewAction {
        return object : ViewAction {
            override fun getDescription() = "with tab at index $tabIndex"

            override fun getConstraints() = allOf(isDisplayed(), isAssignableFrom(TabLayout::class.java))

            override fun perform(uiController: UiController, view: View) {
                val tabLayout = view as TabLayout
                val tabAtIndex: TabLayout.Tab = tabLayout.getTabAt(tabIndex)
                    ?: throw PerformException.Builder()
                        .withCause(Throwable("No tab at index $tabIndex"))
                        .build()

                tabAtIndex.select()
            }
        }
    }
}
