package fr.free.nrw.commons;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import fr.free.nrw.commons.utils.ConfigUtils;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.core.IsNot.not;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WelcomeActivityTest {
    @Rule
    public ActivityTestRule activityRule = new ActivityTestRule<>(WelcomeActivity.class);

    @Test
    public void onlyBetaShowsSkipButton() {
        if (ConfigUtils.isBetaFlavour()) {
            onView(withId(R.id.finishTutorialButton))
                    .check(matches(isDisplayed()));
        } else {
            onView(withId(R.id.finishTutorialButton))
                    .check(matches(not(isDisplayed())));
        }
    }
}