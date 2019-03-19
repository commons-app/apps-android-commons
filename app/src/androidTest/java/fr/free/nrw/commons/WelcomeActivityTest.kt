package fr.free.nrw.commons

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.test.runner.AndroidJUnit4
import fr.free.nrw.commons.utils.ConfigUtils
import org.hamcrest.core.IsNot.not
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class WelcomeActivityTest {
    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(WelcomeActivity::class.java)

    @Test
    fun ifBetaShowsSkipButton() {
        if (ConfigUtils.isBetaFlavour()) {
            onView(withId(R.id.finishTutorialButton))
                    .check(matches(isDisplayed()))
        }
    }

    @Test
    fun ifProdHidesSkipButton() {
        if (!ConfigUtils.isBetaFlavour()) {
            onView(withId(R.id.finishTutorialButton))
                    .check(matches(not(isDisplayed())))
        }
    }
}