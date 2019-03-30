package fr.free.nrw.commons

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.contrib.DrawerActions
import androidx.test.espresso.contrib.NavigationViewActions
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

        // About
        openNavigationDrawerAndNavigateTo(R.id.action_about)

        // Settings
        openNavigationDrawerAndNavigateTo(R.id.action_settings)

        // Achievements
        openNavigationDrawerAndNavigateTo(R.id.action_login)
    }

    private fun openNavigationDrawerAndNavigateTo(menuItemId: Int) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open())
        UITestHelper.sleep(500)
        onView(withId(R.id.navigation_view)).perform(NavigationViewActions.navigateTo(menuItemId))
    }
}