package fr.free.nrw.commons

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowActivity
import java.lang.reflect.Method

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AboutActivityUnitTests {
    private lateinit var activity: AboutActivity
    private lateinit var shadowActivity: ShadowActivity
    private lateinit var context: Context
    private fun invokePrivateLaunchMethod(methodName: String) {
        val method: Method =
            AboutActivity::class.java.getDeclaredMethod(methodName, Context::class.java)
        method.isAccessible = true
        method.invoke(activity, activity)
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        activity = Robolectric.buildActivity(AboutActivity::class.java).create().get()

        context = ApplicationProvider.getApplicationContext()
        shadowActivity = Shadows.shadowOf(activity)
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchFacebook() {
        invokePrivateLaunchMethod("launchFacebook")
        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.action, "android.intent.action.VIEW")
        Assert.assertEquals(startedIntent.`package`, "com.facebook.katana")
        Assert.assertEquals(startedIntent.`data`, Uri.parse("fb://page/1921335171459985"))
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchGithub() {
        invokePrivateLaunchMethod("launchGithub")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchWebsite() {
        invokePrivateLaunchMethod("launchWebsite")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchRatings() {
        invokePrivateLaunchMethod("launchRatings")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchCredits() {
        invokePrivateLaunchMethod("launchCredits")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchPrivacyPolicy() {
        invokePrivateLaunchMethod("launchPrivacyPolicy")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchUserGuide() {
        invokePrivateLaunchMethod("launchUserGuide")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchFrequentlyAskedQuestions() {
        invokePrivateLaunchMethod("launchFrequentlyAskedQuesions")
        Assert.assertEquals(shadowActivity.nextStartedActivity.action, "android.intent.action.VIEW")
    }

    @Test
    @Throws(Exception::class)
    fun testShareApp() {
        val method: Method = AboutActivity::class.java.getDeclaredMethod("shareApp")
        method.isAccessible = true
        method.invoke(activity)

        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.action, Intent.ACTION_SEND)
        Assert.assertEquals(startedIntent.type, "text/plain")
    }
}