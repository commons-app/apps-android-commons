package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.matcher.IntentMatchers
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.utils.ConfigUtils.getVersionNameWithSha
import org.hamcrest.CoreMatchers
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutActivityTest {

    @get:Rule
    var activityRule: ActivityTestRule<*> = ActivityTestRule(AboutActivity::class.java)

    private val device: UiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setup() {
        device.setOrientationNatural()
        device.freezeRotation()
        Intents.init()
        Intents.intending(CoreMatchers.not(IntentMatchers.isInternal()))
            .respondWith(Instrumentation.ActivityResult(Activity.RESULT_OK, null))
    }

    @After
    fun cleanUp() {
        Intents.release()
    }

    @Test
    fun testBuildNumber() {
        Espresso.onView(ViewMatchers.withId(R.id.about_version))
            .check(
                ViewAssertions.matches(
                    withText(getApplicationContext<CommonsApplication>().getVersionNameWithSha())
                )
            )
    }

    @Test
    fun testLaunchWebsite() {
        Espresso.onView(ViewMatchers.withId(R.id.website_launch_icon)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Urls.WEBSITE_URL)
            )
        )
    }

    @Test
    fun testLaunchFacebook() {
        Espresso.onView(ViewMatchers.withId(R.id.facebook_launch_icon)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.anyOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Urls.FACEBOOK_WEB_URL),
                IntentMatchers.hasPackage(Urls.FACEBOOK_PACKAGE_NAME)
            )
        )
    }

    @Test
    fun testLaunchGithub() {
        Espresso.onView(ViewMatchers.withId(R.id.github_launch_icon)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Urls.GITHUB_REPO_URL)
            )
        )
    }

    @Test
    fun testLaunchAboutPrivacyPolicy() {
        Espresso.onView(ViewMatchers.withId(R.id.about_privacy_policy)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(BuildConfig.PRIVACY_POLICY_URL)
            )
        )
    }

    @Test
    fun testLaunchTranslate() {
        Espresso.onView(ViewMatchers.withId(R.id.about_translate)).perform(ViewActions.click())
        Espresso.onView(ViewMatchers.withId(android.R.id.button1)).perform(ViewActions.click())
        val langCode = CommonsApplication.getInstance().languageLookUpTable.codes[0]
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData("${Urls.TRANSLATE_WIKI_URL}$langCode")
            )
        )
    }

    @Test
    fun testLaunchAboutCredits() {
        Espresso.onView(ViewMatchers.withId(R.id.about_credits)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Urls.CREDITS_URL)
            )
        )
    }

    @Test
    fun testLaunchUserGuide() {
        Espresso.onView(ViewMatchers.withId(R.id.about_user_guide)).perform(ViewActions.click())
        Intents.intended(CoreMatchers.allOf(IntentMatchers.hasAction(Intent.ACTION_VIEW),
            IntentMatchers.hasData(Urls.USER_GUIDE_URL)))
    }


    @Test
    fun testLaunchAboutFaq() {
        Espresso.onView(ViewMatchers.withId(R.id.about_faq)).perform(ViewActions.click())
        Intents.intended(
            CoreMatchers.allOf(
                IntentMatchers.hasAction(Intent.ACTION_VIEW),
                IntentMatchers.hasData(Urls.FAQ_URL)
            )
        )
    }
}
