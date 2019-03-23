package fr.free.nrw.commons

import androidx.test.espresso.Espresso
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class NavigationBaseActivityTest {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(AboutActivity::class.java)

    /**
     * Goes through all the activities in the app and checks we don't crash
     * NB: This is not realistic if we're not logged in; we can access 'home', 'notifications', 'settings' and 'achievements' which we wouldn't otherwise be able to.
     */
    @Test
    fun goThroughNavigationBaseActivityActivities() {
        // Home
        openNavigationDrawerAndNavigateTo(R.id.action_home)

        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore)

        // Bookmarks
        openNavigationDrawerAndNavigateTo(R.id.action_bookmarks)

        // Reviews
        openNavigationDrawerAndNavigateTo(R.id.action_review)

        // Settings
        openNavigationDrawerAndNavigateTo(R.id.action_settings)

        // About
        openNavigationDrawerAndNavigateTo(R.id.action_about)

        // Tutorial
        openNavigationDrawerAndNavigateTo(R.id.action_introduction)
        Espresso.pressBack()

        // Achievements
        openNavigationDrawerAndNavigateTo(R.id.action_login)

        // Feedback
        openNavigationDrawerAndNavigateToFeedback(R.id.action_feedback)
    }

    /**
     * Clicks 'Explore' in the navigation drawer twice, then clicks 'home'
     * Testing to avoid regression of #2200
     */
    @Test
    fun doubleNavigateToExploreThenReturnHome() {
        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore)

        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore)

        // Home
        openNavigationDrawerAndNavigateTo(R.id.action_home)
    }

    private fun openNavigationDrawerAndNavigateTo(menuItemId: Int) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.navigation_view)).perform(NavigationViewActions.navigateTo(menuItemId))
    }

    private fun openNavigationDrawerAndNavigateToFeedback(menuItemId: Int) {
        Intents.init()
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        onView(withId(R.id.navigation_view)).perform(NavigationViewActions.navigateTo(menuItemId))
        Intents.release()
    }

}
