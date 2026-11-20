package fr.free.nrw.commons.activity

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import fr.free.nrw.commons.TestCommonsApplication
import fr.free.nrw.commons.activity.SingleWebViewActivity.NavigationDecision
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = TestCommonsApplication::class)
class SingleWebViewActivityTest {
    private val validVanishUrl = "https://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps"
    private val validSuccessUrl = "https://meta.m.wikimedia.org/wiki/Special:GlobalVanishRequest/vanished"

    @Test
    fun `areValidLaunchUrls accepts trusted https wikimedia urls and rejects untrusted or cleartext urls`() {
        assertTrue(SingleWebViewActivity.areValidLaunchUrls(validVanishUrl, validSuccessUrl))
        assertTrue(
            SingleWebViewActivity.areValidLaunchUrls(
                "https://commons.wikimedia.beta.wmflabs.org/wiki/Special:Contact",
                "https://commons.wikimedia.beta.wmflabs.org/wiki/Main_Page",
            ),
        )

        assertFalse(SingleWebViewActivity.areValidLaunchUrls(null, validSuccessUrl))
        assertFalse(SingleWebViewActivity.areValidLaunchUrls(validVanishUrl, null))
        assertFalse(
            SingleWebViewActivity.areValidLaunchUrls(
                "http://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps",
                validSuccessUrl,
            ),
        )
        assertFalse(
            SingleWebViewActivity.areValidLaunchUrls(
                "https://attacker.example.com/phish",
                validSuccessUrl,
            ),
        )
        assertFalse(
            SingleWebViewActivity.areValidLaunchUrls(
                "https://notwikimedia.org/wiki/Special:Contact",
                validSuccessUrl,
            ),
        )
        assertFalse(
            SingleWebViewActivity.areValidLaunchUrls(
                "file:///data/data/fr.free.nrw.commons/shared_prefs/default.xml",
                validSuccessUrl,
            ),
        )
    }

    @Test
    fun `decideNavigation isolates external links and blocks cleartext or unsafe schemes`() {
        assertEquals(
            NavigationDecision.CLOSE_ON_SUCCESS,
            SingleWebViewActivity.decideNavigation(validSuccessUrl, isForMainFrame = true, validSuccessUrl),
        )
        assertEquals(
            NavigationDecision.ALLOW_IN_WEBVIEW,
            SingleWebViewActivity.decideNavigation(
                "https://auth.wikimedia.org/commonswiki/wiki/Special:CentralLogin",
                isForMainFrame = true,
                validSuccessUrl,
            ),
        )
        assertEquals(
            NavigationDecision.OPEN_EXTERNAL_BROWSER,
            SingleWebViewActivity.decideNavigation(
                "https://example.com/external-help",
                isForMainFrame = true,
                validSuccessUrl,
            ),
        )
        assertEquals(
            NavigationDecision.BLOCK,
            SingleWebViewActivity.decideNavigation(
                "https://example.com/subframe",
                isForMainFrame = false,
                validSuccessUrl,
            ),
        )
        assertEquals(
            NavigationDecision.BLOCK,
            SingleWebViewActivity.decideNavigation(
                "http://meta.m.wikimedia.org/wiki/Main_Page",
                isForMainFrame = true,
                validSuccessUrl,
            ),
        )
        assertEquals(
            NavigationDecision.BLOCK,
            SingleWebViewActivity.decideNavigation(
                "javascript:alert(document.cookie)",
                isForMainFrame = true,
                validSuccessUrl,
            ),
        )
    }

    @Test
    fun `formatCookieForWebView enforces Secure and HttpOnly and scopes cross-subdomain cookies safely`() {
        val commonsCentralAuth =
            Cookie
                .Builder()
                .name("centralauth_Token")
                .value("secret123")
                .domain("commons.wikimedia.org")
                .path("/")
                .build()
        val metaTarget = validVanishUrl.toHttpUrl()

        val formattedCrossSubdomain = SingleWebViewActivity.formatCookieForWebView(commonsCentralAuth, metaTarget)
        assertTrue(formattedCrossSubdomain.contains("centralauth_Token=secret123"))
        assertTrue(formattedCrossSubdomain.lowercase().contains("secure"))
        assertTrue(formattedCrossSubdomain.lowercase().contains("httponly"))
        // Must be host-only for meta.m.wikimedia.org rather than mismatched domain=commons.wikimedia.org
        assertFalse(formattedCrossSubdomain.contains("domain=commons.wikimedia.org"))

        val rootWikimediaCookie =
            Cookie
                .Builder()
                .name("centralauth_Session")
                .value("sess456")
                .domain("wikimedia.org")
                .path("/")
                .build()
        val formattedRootDomain = SingleWebViewActivity.formatCookieForWebView(rootWikimediaCookie, metaTarget)
        assertTrue(formattedRootDomain.contains("domain=wikimedia.org"))
        assertTrue(formattedRootDomain.lowercase().contains("secure"))
        assertTrue(formattedRootDomain.lowercase().contains("httponly"))
    }

    @Test
    fun `onCreate finishes immediately when launched with untrusted or cleartext url`() {
        val context = ApplicationProvider.getApplicationContext<TestCommonsApplication>()
        val untrustedIntent =
            Intent(context, SingleWebViewActivity::class.java).apply {
                putExtra("VanishAccountUrl", "https://evil.example.com/")
                putExtra("vanishAccountSuccessUrl", validSuccessUrl)
            }
        val controller = Robolectric.buildActivity(SingleWebViewActivity::class.java, untrustedIntent).create()
        assertTrue(controller.get().isFinishing)
    }
}
