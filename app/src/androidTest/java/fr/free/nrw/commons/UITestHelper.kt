package fr.free.nrw.commons

import android.app.Activity
import android.content.pm.ActivityInfo
import android.text.method.Touch.scrollTo
import android.view.View
import android.view.ViewGroup
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import org.apache.commons.lang3.StringUtils
import org.hamcrest.*
import timber.log.Timber


class UITestHelper {
    companion object {
        fun skipWelcome() {
            try {
                //Skip tutorial
                onView(ViewMatchers.withId(R.id.finishTutorialButton))
                    .perform(ViewActions.click())
            } catch (ignored: NoMatchingViewException) {
            }
        }

        fun loginUser() {
            try {
                //Perform Login
                sleep(3000)
                onView(ViewMatchers.withId(R.id.login_username))
                    .perform(
                        ViewActions.replaceText(getTestUsername()),
                        ViewActions.closeSoftKeyboard()
                    )
                sleep(2000)
                onView(ViewMatchers.withId(R.id.login_password))
                    .perform(
                        ViewActions.replaceText(getTestUserPassword()),
                        ViewActions.closeSoftKeyboard()
                    )
                sleep(2000)
                onView(ViewMatchers.withId(R.id.login_button))
                    .perform(ViewActions.click())
                sleep(10000)
            } catch (ignored: NoMatchingViewException) {
            }

        }

        fun logoutUser() {
            try {
                sleep(3000)
                onView(
                    Matchers.allOf(
                        ViewMatchers.withContentDescription("More"),
                        childAtPosition(
                            childAtPosition(
                                ViewMatchers.withId(R.id.fragment_main_nav_tab_layout),
                                0
                            ),
                            4
                        ),
                        ViewMatchers.isDisplayed()
                    )
                ).perform(ViewActions.click())
                sleep(3000)
                onView(
                    Matchers.allOf(
                        ViewMatchers.withId(R.id.more_logout), ViewMatchers.withText("Logout"),
                    )
                ).perform(ViewActions.click())
                onView(
                    Matchers.allOf(
                        ViewMatchers.withId(android.R.id.button1), ViewMatchers.withText("Yes"),
                    )
                ).perform(ViewActions.click())
                sleep(10000)
            } catch (ignored: NoMatchingViewException) {
            }
        }

        private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int
        ): Matcher<View> {

            return object : TypeSafeMatcher<View>() {
                override fun describeTo(description: Description) {
                    description.appendText("Child at position $position in parent ")
                    parentMatcher.describeTo(description)
                }

                public override fun matchesSafely(view: View): Boolean {
                    val parent = view.parent
                    return parent is ViewGroup && parentMatcher.matches(parent)
                            && view == parent.getChildAt(position)
                }
            }
        }

        fun sleep(timeInMillis: Long) {
            try {
                Timber.d("Sleeping for %d", timeInMillis)
                Thread.sleep(timeInMillis)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun getTestUsername(): String {
            val username = BuildConfig.TEST_USERNAME
            if (StringUtils.isEmpty(username) || username == "null") {
                throw NotImplementedError("Configure your beta account's username")
            } else return username
        }

        private fun getTestUserPassword(): String {
            val password = BuildConfig.TEST_PASSWORD
            if (StringUtils.isEmpty(password) || password == "null") {
                throw NotImplementedError("Configure your beta account's password")
            } else return password
        }

        fun <T : Activity> changeOrientation(activityRule: ActivityTestRule<T>) {
            activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }

        fun <T> first(matcher: Matcher<T>): Matcher<T>? {
            return object : BaseMatcher<T>() {
                var isFirst = true
                override fun matches(item: Any): Boolean {
                    if (isFirst && matcher.matches(item)) {
                        isFirst = false
                        return true
                    }
                    return false
                }

                override fun describeTo(description: Description) {
                    description.appendText("should return first matching item")
                }
            }
        }
    }
}