package fr.free.nrw.commons

import androidx.test.espresso.Espresso.closeSoftKeyboard
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.NoMatchingViewException
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers
import fr.free.nrw.commons.utils.StringUtils
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
                onView(ViewMatchers.withId(R.id.login_username))
                        .perform(ViewActions.clearText(), ViewActions.typeText(getTestUsername()))
                onView(ViewMatchers.withId(R.id.login_password))
                        .perform(ViewActions.clearText(), ViewActions.typeText(getTestUserPassword()))
                closeSoftKeyboard()
                onView(ViewMatchers.withId(R.id.login_button))
                        .perform(ViewActions.click())
                sleep(5000)
            } catch (ignored: NoMatchingViewException) {
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
            if (StringUtils.isNullOrWhiteSpace(username) || username == "null") {
                throw NotImplementedError("Configure your beta account's username")
            } else return username
        }

        private fun getTestUserPassword(): String {
            val password = BuildConfig.TEST_PASSWORD
            if (StringUtils.isNullOrWhiteSpace(password) || password == "null") {
                throw NotImplementedError("Configure your beta account's password")
            } else return password
        }
    }
}