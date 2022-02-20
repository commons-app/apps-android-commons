package fr.free.nrw.commons

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import androidx.viewpager.widget.ViewPager
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
        if (ConfigUtils.isBetaFlavour) {
            onView(withId(R.id.finishTutorialButton))
                .check(matches(isDisplayed()))
        }
    }

    @Test
    fun ifProdHidesSkipButton() {
        if (!ConfigUtils.isBetaFlavour) {
            onView(withId(R.id.finishTutorialButton))
                .check(matches(not(isDisplayed())))
        }
    }

    @Test
    fun testBetaSkipButton() {
        if (ConfigUtils.isBetaFlavour) {
            onView(withId(R.id.finishTutorialButton))
                .perform(ViewActions.click())
            assert(activityRule.activity.isDestroyed)
        }
    }

    @Test
    fun testSwipingOnce() {
        onView(withId(R.id.welcomePager))
            .perform(ViewActions.swipeLeft())
        assert(true)
        onView(withId(R.id.welcomePager))
            .perform(ViewActions.swipeRight())
        assert(true)
    }

    @Test
    fun testSwipingWholeTutorial() {
        onView(withId(R.id.welcomePager))
            .perform(ViewActions.swipeLeft())
            .perform(ViewActions.swipeLeft())
            .perform(ViewActions.swipeLeft())
            .perform(ViewActions.swipeLeft())
        assert(true)
        onView(withId(R.id.welcomePager))
            .perform(ViewActions.swipeRight())
            .perform(ViewActions.swipeRight())
            .perform(ViewActions.swipeRight())
            .perform(ViewActions.swipeRight())
        assert(true)
    }

    @Test
    fun swipeBeyondBounds() {
        val viewPager = activityRule.activity.findViewById<ViewPager>(R.id.welcomePager)

        viewPager.adapter?.let {
            if (viewPager.currentItem == 3) {
                onView(withId(R.id.welcomePager))
                    .perform(ViewActions.swipeLeft())
                assert(true)
                onView(withId(R.id.welcomePager))
                    .perform(ViewActions.swipeRight())
                assert(false)
            }
        }
    }

    @Test
    fun swipeTillLastAndFinish() {
        val viewPager = activityRule.activity.findViewById<ViewPager>(R.id.welcomePager)

        viewPager.adapter?.let {
            if (viewPager.currentItem == 3) {
                onView(withId(R.id.finishTutorialButton))
                    .perform(ViewActions.click())
                assert(activityRule.activity.isDestroyed)
            }
        }
    }

    @Test
    fun orientationChange() {
        UITestHelper.changeOrientation(activityRule)
    }
}