package fr.free.nrw.commons

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import fr.free.nrw.commons.utils.StringUtils
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
            }
        }

        fun loginUser() {
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

            try {
                onView(ViewMatchers.withId(R.id.login_username))
                        .perform(ViewActions.clearText(), ViewActions.typeText(BuildConfig.TEST_USERNAME))
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
            return !(StringUtils.isNullOrWhiteSpace(BuildConfig.TEST_USERNAME)
                    || BuildConfig.TEST_USERNAME == "null")
        }
    }
}