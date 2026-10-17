package fr.free.nrw.commons.auth

import android.app.Activity
import android.webkit.WebView
import fr.free.nrw.commons.BuildConfig
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.powermock.reflect.Whitebox
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class SignupActivityTest {
    private lateinit var activity: SignupActivity

    @Mock
    private lateinit var webView: WebView

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        activity = Robolectric.buildActivity(SignupActivity::class.java).create().get()
    }

    @Test
    @Throws(Exception::class)
    fun checkActivityNotNull() {
        Assert.assertNotNull(activity)
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressed() {
        activity.onBackPressed()
    }

    @Test
    @Throws(Exception::class)
    fun testOnBackPressedCaseCanGoBack() {
        Whitebox.setInternalState(activity, "webView", webView)
        `when`(webView.canGoBack()).thenReturn(true)
        activity.onBackPressed()
    }

    @Test
    fun testExtractUsernameFromCookies() {
        Assert.assertEquals(
            "New User",
            SignupActivity.extractUsername(
                "GeoIP=XX; commonswikiUserName=New_User; commonswikiSession=abc"
            )
        )
        Assert.assertEquals(
            "Ünïcode User",
            SignupActivity.extractUsername("commonswikiUserName=%C3%9Cn%C3%AFcode+User")
        )
    }

    @Test
    fun testExtractUsernameWithoutSession() {
        Assert.assertNull(SignupActivity.extractUsername(null))
        Assert.assertNull(SignupActivity.extractUsername("GeoIP=XX; commonswikiSession=abc"))
    }

    @Test
    fun testOtherPagesDoNotFinishSignup() {
        Assert.assertFalse(
            activity.handleSignupSuccessIfNeeded(
                "https://commons.m.wikimedia.org/wiki/Special:CreateAccount"
            )
        )
        Assert.assertFalse(activity.isFinishing)
    }

    @Test
    fun testSuccessRedirectFinishesSignup() {
        Assert.assertTrue(
            activity.handleSignupSuccessIfNeeded(BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL)
        )
        Assert.assertTrue(activity.isFinishing)
        Assert.assertEquals(Activity.RESULT_OK, shadowOf(activity).resultCode)
    }

    @Test
    fun testSignupSuccessUrlDetection() {
        Assert.assertTrue(
            SignupActivity.isSignupSuccessUrl(
                "https://commons.wikimedia.org/w/index.php?title=Main_Page&welcome=yes&accountJustCreated=1"
            )
        )
        Assert.assertTrue(
            SignupActivity.isSignupSuccessUrl(BuildConfig.SIGNUP_SUCCESS_REDIRECTION_URL)
        )
        Assert.assertFalse(
            SignupActivity.isSignupSuccessUrl(
                "https://auth.wikimedia.org/commonswiki/wiki/Special:CreateAccount?returnto=Main+Page&returntoquery=welcome%3Dyes"
            )
        )
        Assert.assertFalse(
            SignupActivity.isSignupSuccessUrl("https://example.org/?accountJustCreated=1")
        )
    }
}
