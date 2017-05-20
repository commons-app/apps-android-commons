package fr.free.nrw.commons;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

import android.support.test.espresso.assertion.ViewAssertions;
import android.support.test.filters.LargeTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import fr.free.nrw.commons.nearby.NearbyActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class NearbyActivityTest {
    @Rule
    public final ActivityTestRule<NearbyActivity> nearby =
            new ActivityTestRule<>(NearbyActivity.class);

    @Test
    public void testActivityLaunch() {
        onView(withText("Nearby Places")).check(ViewAssertions.matches(isDisplayed()));
    }
}
