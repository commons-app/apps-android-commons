package fr.free.nrw.commons;


import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.view.Gravity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import fr.free.nrw.commons.contributions.ContributionsActivity;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.contrib.DrawerActions.open;
import static android.support.test.espresso.contrib.DrawerActions.close;
import static android.support.test.espresso.contrib.DrawerMatchers.isClosed;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class NavigationDrawerTest {

    @Rule
    public ActivityTestRule<ContributionsActivity> mActivityRule = new ActivityTestRule<>(
            ContributionsActivity.class);


    @Test
    public void testNavigationDrawer(){
        onView(withId(R.id.drawer_layout))
                .check(matches(isClosed(Gravity.LEFT)))
                .perform(open());

        //test the settings fragment is displayed
        onView(withId(R.id.settings_item)).perform(click());
        onView(withId(R.id.settingsFragment)).check(matches(isDisplayed()));

        onView(withId(R.id.drawer_layout))
                .perform(close());

        //test the nearby item fragment is displayed
        onView(withId(R.id.drawer_layout))
                .perform(open());
        onView(withId(R.id.nearby_item)).perform(click());
        onView(withId(R.id.listView)).check(matches(isDisplayed()));

        onView(withId(R.id.drawer_layout))
                .perform(close());


    }

}
