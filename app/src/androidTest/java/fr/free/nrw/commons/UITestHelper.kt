package fr.free.nrw.commons

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.PerformException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.rule.ActivityTestRule
import fr.free.nrw.commons.utils.ConfigUtils
import org.apache.commons.lang3.StringUtils
import org.hamcrest.CoreMatchers.allOf
import org.junit.Assume
import timber.log.Timber

class UITestHelper {
    companion object {
        fun skipWelcome() {
            try {
                //Skip tutorial
                onView(ViewMatchers.withId(R.id.finishTutorialButton))
                        .perform(ViewActions.click())
            } catch (ignored: NoMatchingViewException) {
            } catch (ignored: PerformException) {
                onView(ViewMatchers.withId(R.id.welcomePager))
                        .perform(ViewActions.swipeLeft())
                        .perform(ViewActions.swipeLeft())
                        .perform(ViewActions.swipeLeft())
                        .perform(ViewActions.swipeLeft())
                onView(allOf(
                        ViewMatchers.withId(R.id.finishTutorialButton),
                        ViewMatchers.withText("YES!")))
                        .perform(ViewActions.click())
            }
        }

        fun loginUser() {
            checkShouldLogin()

            try {
                onView(ViewMatchers.withId(R.id.login_username))
                        .perform(ViewActions.clearText(), ViewActions.typeText(BuildConfig.TEST_USERNAME))
                closeSoftKeyboard()
                onView(ViewMatchers.withId(R.id.login_password))
                        .perform(ViewActions.clearText(), ViewActions.typeText(BuildConfig.TEST_PASSWORD))
                closeSoftKeyboard()
                onView(ViewMatchers.withId(R.id.login_button))
                        .perform(ViewActions.click())
                sleep(5000)
            } catch (ignored: NoMatchingViewException) {}
        }

        fun sleep(timeInMillis: Long) {
            try {
                Timber.d("Sleeping for %d", timeInMillis)
                Thread.sleep(timeInMillis)
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }

        private fun credentialIsSet(credential: String): Boolean {
            return !(StringUtils.isWhitespace(credential) || credential == "null")
        }

        private fun checkShouldLogin() {
            Assume.assumeTrue(
                    "Tests requiring login should only be performed on Beta Commons",
                    ConfigUtils.isBetaFlavour())

            Assume.assumeTrue(
                    "Beta account username not set\n" +
                    "This can be done in the build config of app/build.gradle or by exporting the environment variable test_user_name\n" +
                    "This message is expected on PR builds on Travis",
                    credentialIsSet(BuildConfig.TEST_USERNAME))

            Assume.assumeTrue(
                    "Beta account password not set\n" +
                    "This can be done in the build config of app/build.gradle or by exporting the environment variable test_user_password\n" +
                    "This message is expected on PR builds on Travis",
                    credentialIsSet(BuildConfig.TEST_PASSWORD))
        }
      
        fun <T: Activity> changeOrientation(activityRule: ActivityTestRule<T>){
            activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            activityRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            assert(activityRule.activity.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
        }
    }
}