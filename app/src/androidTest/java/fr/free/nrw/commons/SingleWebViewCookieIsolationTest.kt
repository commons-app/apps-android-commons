package fr.free.nrw.commons

import android.app.Activity
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebView
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import fr.free.nrw.commons.activity.SingleWebViewActivity
import fr.free.nrw.commons.auth.LoginActivity
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SingleWebViewCookieIsolationTest {
    private val device: UiDevice =
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

    @Before
    fun setUp() {
        device.wakeUp()
        clearAllCookies()
        seedSyntheticSessionCookies()
    }

    @After
    fun tearDown() {
        clearAllCookies()
    }

    @Test
    fun scenario1_okHttpCrossOriginRedirectAndCleartextIsolation() {
        val capturedHeaders = CopyOnWriteArrayList<Pair<String, String?>>()
        val networkInterceptor =
            Interceptor { chain ->
                val req = chain.request()
                capturedHeaders.add(req.url.toString() to req.header("Cookie"))
                val sanitized = req.newBuilder().removeHeader("Cookie").build()
                val realResponse = chain.proceed(sanitized)
                maybeSimulateCrossOriginRedirect(req, realResponse)
            }
        val client =
            OkHttpClient
                .Builder()
                .cookieJar(CommonsApplication.instance.cookieJar)
                .addNetworkInterceptor(networkInterceptor)
                .build()

        listOf(
            "https://commons.wikimedia.org/w/api.php?action=query&meta=siteinfo&format=json",
            "https://www.wikidata.org/w/api.php?action=query&meta=siteinfo&format=json",
            "https://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&format=json",
            "https://commons.wikimedia.org/w/api.php?action=query&meta=siteinfo&format=json&test_redirect=1",
        ).forEach { targetUrl ->
            client.newCall(Request.Builder().url(targetUrl).build()).execute().close()
        }

        val byUrl = capturedHeaders.toMap()
        val commonsUrl = "https://commons.wikimedia.org/w/api.php?action=query&meta=siteinfo&format=json"
        val commonsCookie = byUrl[commonsUrl].orEmpty()
        assertTrue(commonsCookie.contains("centralauth_Token=$SYNTHETIC_TOKEN"))
        assertTrue(commonsCookie.contains("commonswikiSession=$SYNTHETIC_LOCAL_SESSION"))

        val wikidataUrl = "https://www.wikidata.org/w/api.php?action=query&meta=siteinfo&format=json"
        val wikidataCookie = byUrl[wikidataUrl].orEmpty()
        assertTrue(wikidataCookie.contains("centralauth_Token=$SYNTHETIC_TOKEN"))
        assertFalse(wikidataCookie.contains("commonswikiSession"))

        val wikipediaUrl = "https://en.wikipedia.org/w/api.php?action=query&meta=siteinfo&format=json"
        val wikipediaCookie = byUrl[wikipediaUrl].orEmpty()
        assertTrue(wikipediaCookie.contains("centralauth_Session=$SYNTHETIC_SESSION"))
        assertFalse(wikipediaCookie.contains("commonswikiSession"))

        assertTrue(byUrl.containsKey("https://example.com/"))
        assertNull(byUrl["https://example.com/"])

        val jar = CommonsApplication.instance.cookieJar
        assertTrue(jar.loadForRequest("https://tools.wmflabs.org/commons-android-app".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("https://notwikimedia.org/w/api.php".toHttpUrl()).isEmpty())
        assertTrue(jar.loadForRequest("http://commons.wikimedia.org/w/api.php".toHttpUrl()).isEmpty())
    }

    @Test
    fun scenario2_singleWebViewLoadsVanishPageWithSecureHttpOnlyCookies() {
        val intent = createVanishIntent(VANISH_URL, VANISH_SUCCESS_URL)
        ActivityScenario.launch<SingleWebViewActivity>(intent).use { scenario ->
            val webView = awaitLoadedWebView(scenario)
            assertNotNull(webView)
            val cacheDir = ApplicationProvider.getApplicationContext<Context>().externalCacheDir
            if (cacheDir != null) {
                device.takeScreenshot(File(cacheDir, "sec05_vanish_webview.png"))
            }

            val cookieManager = CookieManager.getInstance()
            val httpsCookies = cookieManager.getCookie(VANISH_URL).orEmpty()
            assertTrue(httpsCookies.contains("centralauth_Token=$SYNTHETIC_TOKEN"))
            assertTrue(httpsCookies.contains("centralauth_Session=$SYNTHETIC_SESSION"))
            assertFalse(httpsCookies.contains("commonswikiSession"))

            val httpCookies =
                cookieManager.getCookie("http://meta.m.wikimedia.org/wiki/Special:Contact").orEmpty()
            assertFalse(httpCookies.contains(SYNTHETIC_TOKEN))
            assertFalse(httpCookies.contains(SYNTHETIC_SESSION))

            val jsCookieRef = AtomicReference("")
            val jsLatch = CountDownLatch(1)
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                webView!!.evaluateJavascript("document.cookie") { value ->
                    jsCookieRef.set(value.orEmpty())
                    jsLatch.countDown()
                }
            }
            assertTrue(jsLatch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS))
            assertFalse(jsCookieRef.get().contains(SYNTHETIC_TOKEN))
            assertFalse(jsCookieRef.get().contains(SYNTHETIC_SESSION))
        }
    }

    @Test
    fun scenario3_untrustedCleartextAndSpoofedIntentsRejectedWithZeroCookies() {
        val hostileUrls =
            listOf(
                "https://attacker.example.com/phish",
                "https://notwikimedia.org/wiki/Special:Contact",
                "http://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps",
                "https://tools.wmflabs.org/evil",
                "file:///data/data/fr.free.nrw.commons/shared_prefs/fr.free.nrw.commons_preferences.xml",
            )
        hostileUrls.forEach { badUrl ->
            val intent = createVanishIntent(badUrl, VANISH_SUCCESS_URL)
            ActivityScenario.launch<SingleWebViewActivity>(intent).use { scenario ->
                Thread.sleep(SHORT_WAIT_MS)
                assertEquals(Lifecycle.State.DESTROYED, scenario.state)
            }
        }
        val cookieManager = CookieManager.getInstance()
        assertNull(cookieManager.getCookie("https://attacker.example.com/"))
        assertNull(cookieManager.getCookie("https://notwikimedia.org/"))
        assertNull(cookieManager.getCookie("https://tools.wmflabs.org/"))
    }

    @Test
    fun scenario4_externalLinkClickOpensExternalBrowserWithoutCookieLeak() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val viewFilter =
            IntentFilter(Intent.ACTION_VIEW).apply {
                addDataScheme("https")
            }
        val monitor =
            instrumentation.addMonitor(
                viewFilter,
                Instrumentation.ActivityResult(Activity.RESULT_OK, null),
                true,
            )
        try {
            val intent = createVanishIntent(VANISH_URL, VANISH_SUCCESS_URL)
            ActivityScenario.launch<SingleWebViewActivity>(intent).use { scenario ->
                val webView = awaitLoadedWebView(scenario)
                assertNotNull(webView)
                clickAnchorInWebView(webView!!, EXTERNAL_CC_URL)
                Thread.sleep(NAVIGATION_WAIT_MS)
                assertEquals(1, monitor.hits)
                assertNull(CookieManager.getInstance().getCookie(EXTERNAL_CC_URL))
                val currentUrlRef = AtomicReference("")
                instrumentation.runOnMainSync { currentUrlRef.set(webView.url.orEmpty()) }
                assertTrue(currentUrlRef.get().contains("wikimedia.org"))
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }

    @Test
    fun scenario5_vanishSuccessUrlClearsCookieJarAndWebViewCookies() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val loginMonitor =
            instrumentation.addMonitor(
                LoginActivity::class.java.name,
                Instrumentation.ActivityResult(Activity.RESULT_OK, null),
                true,
            )
        try {
            val intent = createVanishIntent(VANISH_URL, VANISH_SUCCESS_URL)
            ActivityScenario.launch<SingleWebViewActivity>(intent).use { scenario ->
                val webView = awaitLoadedWebView(scenario)
                assertNotNull(webView)
                clickAnchorInWebView(webView!!, VANISH_SUCCESS_URL)
                val jar = CommonsApplication.instance.cookieJar
                val commonsHttpUrl = "https://commons.wikimedia.org/".toHttpUrl()
                val deadline = System.currentTimeMillis() + LOGOUT_WAIT_MS
                while (System.currentTimeMillis() < deadline && jar.loadForRequest(commonsHttpUrl).isNotEmpty()) {
                    Thread.sleep(POLL_INTERVAL_MS)
                }
                assertEquals(emptyList<Cookie>(), jar.loadForRequest(commonsHttpUrl))
                val remainingWebViewCookies = CookieManager.getInstance().getCookie(VANISH_URL).orEmpty()
                assertFalse(remainingWebViewCookies.contains(SYNTHETIC_TOKEN))
                assertFalse(remainingWebViewCookies.contains(SYNTHETIC_SESSION))
            }
        } finally {
            instrumentation.removeMonitor(loginMonitor)
        }
    }

    private fun seedSyntheticSessionCookies() {
        val commonsUrl = "https://commons.wikimedia.org/w/api.php".toHttpUrl()
        val cookies =
            listOf(
                Cookie.parse(
                    commonsUrl,
                    "centralauth_Token=$SYNTHETIC_TOKEN; Domain=commons.wikimedia.org; Path=/; Secure; HttpOnly",
                )!!,
                Cookie.parse(
                    commonsUrl,
                    "centralauth_Session=$SYNTHETIC_SESSION; Path=/",
                )!!,
                Cookie.parse(
                    commonsUrl,
                    "commonswikiSession=$SYNTHETIC_LOCAL_SESSION; Path=/; Secure; HttpOnly",
                )!!,
            )
        CommonsApplication.instance.cookieJar.saveFromResponse(commonsUrl, cookies)
    }

    private fun clearAllCookies() {
        CommonsApplication.instance.cookieJar.clear()
        val latch = CountDownLatch(1)
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val cm = CookieManager.getInstance()
            cm.removeAllCookies { latch.countDown() }
            cm.flush()
        }
        latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)
    }

    companion object {
        private const val VANISH_URL = "https://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps"
        private const val VANISH_SUCCESS_URL = "https://meta.m.wikimedia.org/wiki/Special:GlobalVanishRequest/vanished"
        private const val EXTERNAL_CC_URL = "https://creativecommons.org/licenses/by-sa/4.0/"
        private const val SYNTHETIC_TOKEN = "SYNTHETIC_SSO_SECRET_05"
        private const val SYNTHETIC_SESSION = "SYNTHETIC_SSO_SESS_05"
        private const val SYNTHETIC_LOCAL_SESSION = "SYNTHETIC_LOCAL_COMMONS_05"
        private const val TIMEOUT_SECONDS = 5L
        private const val SHORT_WAIT_MS = 500L
        private const val NAVIGATION_WAIT_MS = 1500L
        private const val LOGOUT_WAIT_MS = 6000L
    }
}

private const val HTTP_FOUND = 302
private const val COMPLETE_PROGRESS = 100
private const val POLL_INTERVAL_MS = 250L
private const val WEBVIEW_LOAD_TIMEOUT_MS = 12000L

private fun createVanishIntent(
    url: String,
    successUrl: String,
): Intent =
    Intent(ApplicationProvider.getApplicationContext(), SingleWebViewActivity::class.java).apply {
        putExtra("VanishAccountUrl", url)
        putExtra("vanishAccountSuccessUrl", successUrl)
    }

private fun clickAnchorInWebView(
    webView: WebView,
    href: String,
) {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        webView.evaluateJavascript(
            "(function(){var a=document.createElement('a');" +
                "a.href='$href';document.body.appendChild(a);a.click();})()",
            null,
        )
    }
}

private fun maybeSimulateCrossOriginRedirect(
    req: Request,
    realResponse: Response,
): Response =
    if (req.url.queryParameter("test_redirect") == "1") {
        realResponse
            .newBuilder()
            .code(HTTP_FOUND)
            .message("Found")
            .header("Location", "https://example.com/")
            .build()
    } else {
        realResponse
    }

private fun awaitLoadedWebView(scenario: ActivityScenario<SingleWebViewActivity>): WebView? {
    val webViewRef = AtomicReference<WebView?>()
    val deadline = System.currentTimeMillis() + WEBVIEW_LOAD_TIMEOUT_MS
    while (System.currentTimeMillis() < deadline) {
        var progress = 0
        scenario.onActivity { activity ->
            val found = findWebView(activity.window.decorView)
            webViewRef.set(found)
            progress = found?.progress ?: 0
        }
        if (webViewRef.get() != null && progress >= COMPLETE_PROGRESS) {
            break
        }
        Thread.sleep(POLL_INTERVAL_MS)
    }
    return webViewRef.get()
}

private fun findWebView(root: View?): WebView? =
    when (root) {
        is WebView -> root
        is ViewGroup ->
            (0 until root.childCount)
                .asSequence()
                .mapNotNull { findWebView(root.getChildAt(it)) }
                .firstOrNull()
        else -> null
    }
