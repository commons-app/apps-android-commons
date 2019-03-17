package fr.free.nrw.commons;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.espresso.contrib.DrawerActions;
import androidx.test.espresso.contrib.NavigationViewActions;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NavigationBaseActivityTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(AboutActivity.class);

    /**
     * Goes through all the activities in the app and checks we don't crash
     * NB: This is not realistic if we're not logged in; we can access 'home', 'notifications', 'settings' and 'achievements' which we wouldn't otherwise be able to.
     */
    @Test
    public void goThroughNavigationBaseActivityActivities() {
        // Home
        openNavigationDrawerAndNavigateTo(R.id.action_home);

        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore);

        // Bookmarks
        openNavigationDrawerAndNavigateTo(R.id.action_bookmarks);

        // About
        openNavigationDrawerAndNavigateTo(R.id.action_about);

        // Settings
        openNavigationDrawerAndNavigateTo(R.id.action_settings);

        // Achievements
        openNavigationDrawerAndNavigateTo(R.id.action_login);
    }

    /**
     * Clicks 'Explore' in the navigation drawer twice, then clicks 'home'
     * Testing to avoid regression of #2200
     */
    @Test
    public void doubleNavigateToExploreThenReturnHome() {
        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore);

        // Explore
        openNavigationDrawerAndNavigateTo(R.id.action_explore);

        // Home
        openNavigationDrawerAndNavigateTo(R.id.action_home);
    }

    public void openNavigationDrawerAndNavigateTo(int menuItemId) {
        onView(withId(R.id.drawer_layout)).perform(DrawerActions.open());
        onView(withId(R.id.navigation_view)).perform(NavigationViewActions.navigateTo(menuItemId));
    }
}