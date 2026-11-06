package fr.free.nrw.commons

import android.content.ActivityNotFoundException
import android.content.Intent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.spy
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/** Verifies browser fallbacks when external applications cannot be opened. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class AboutLinkHandlerTest {
    /** Falls back to the Facebook web page if the Facebook app cannot open. */
    @Test
    fun facebookFallsBackToBrowser() {
        val activity = spy(Robolectric.buildActivity(AboutActivity::class.java).create().get())
        doThrow(ActivityNotFoundException())
            .doCallRealMethod()
            .`when`(activity)
            .startActivity(any(Intent::class.java))

        AboutLinkHandler(activity).launchFacebook(null)

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Urls.FACEBOOK_WEB_URL, intent.data.toString())
        assertNull(intent.`package`)
    }

    /** Falls back to the repository in a browser if the GitHub app cannot open. */
    @Test
    fun githubFallsBackToBrowser() {
        val activity = spy(Robolectric.buildActivity(AboutActivity::class.java).create().get())
        doThrow(ActivityNotFoundException())
            .doCallRealMethod()
            .`when`(activity)
            .startActivity(any(Intent::class.java))

        AboutLinkHandler(activity).launchGithub(null)

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Urls.GITHUB_REPO_URL, intent.data.toString())
        assertNull(intent.`package`)
    }

    /** Falls back to the Play Store web page if no market application can open. */
    @Test
    fun ratingsFallBackToBrowser() {
        val activity = spy(Robolectric.buildActivity(AboutActivity::class.java).create().get())
        doThrow(ActivityNotFoundException())
            .doCallRealMethod()
            .`when`(activity)
            .startActivity(any(Intent::class.java))

        AboutLinkHandler(activity).launchRatings(null)

        val intent = shadowOf(activity).nextStartedActivity
        assertEquals(Intent.ACTION_VIEW, intent.action)
        assertEquals(Urls.PLAY_STORE_URL_PREFIX + activity.packageName, intent.data.toString())
    }

    /** Propagates errors rather than treating them as missing external applications. */
    @Test(expected = AssertionError::class)
    fun facebookDoesNotSwallowErrors() {
        val activity = spy(Robolectric.buildActivity(AboutActivity::class.java).create().get())
        doThrow(AssertionError("Unexpected error"))
            .doCallRealMethod()
            .`when`(activity)
            .startActivity(any(Intent::class.java))

        AboutLinkHandler(activity).launchFacebook(null)
    }
}
