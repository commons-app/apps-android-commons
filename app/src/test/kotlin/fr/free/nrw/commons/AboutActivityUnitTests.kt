package fr.free.nrw.commons

import android.content.Context
import android.net.Uri
import android.view.Menu
import android.view.MenuItem
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
import org.robolectric.fakes.RoboMenu
import org.robolectric.fakes.RoboMenuItem
import org.robolectric.shadows.ShadowActivity


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AboutActivityUnitTests {

    private lateinit var activity: AboutActivity

    private lateinit var context: Context

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        activity = Robolectric.buildActivity(AboutActivity::class.java).create().get()

        context = ApplicationProvider.getApplicationContext()
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchFacebook() {
        activity.launchFacebook(null)
        val shadowActivity: ShadowActivity = Shadows.shadowOf(activity)
        val startedIntent = shadowActivity.nextStartedActivity
        Assert.assertEquals(startedIntent.action, "android.intent.action.VIEW")
        Assert.assertEquals(startedIntent.`package`, "com.facebook.katana")
        Assert.assertEquals(startedIntent.`data`, Uri.parse("fb://page/1921335171459985"))
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchGithub() {
        activity.launchGithub(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchWebsite() {
        activity.launchWebsite(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchRatings() {
        activity.launchRatings(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchCredits() {
        activity.launchCredits(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchPrivacyPolicy() {
        activity.launchPrivacyPolicy(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchUserGuide() {
        activity.launchUserGuide(null)
    }

    @Test
    @Throws(Exception::class)
    fun testLaunchFrequentlyAskedQuestions() {
        activity.launchFrequentlyAskedQuesions(null)
    }

    @Test
    @Throws(Exception::class)
    fun testOnCreateOptionsMenu() {
        val menu: Menu = RoboMenu(context)
        activity.onCreateOptionsMenu(menu)
    }

    @Test
    @Throws(Exception::class)
    fun testOnOptionsItemSelected() {
        val menuItem: MenuItem = RoboMenuItem(R.menu.menu_about)
        activity.onOptionsItemSelected(menuItem)
        val shadowActivity = Shadows.shadowOf(activity)
        shadowActivity.clickMenuItem(R.id.share_app_icon)
    }

    @Test
    @Throws(Exception::class)
    fun testOnSupportNavigateUp() {
        activity.onSupportNavigateUp()
    }

}