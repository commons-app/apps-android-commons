package fr.free.nrw.commons.wikidata.cookies

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.verify
import fr.free.nrw.commons.kvstore.JsonKvStore
import okhttp3.Cookie
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CommonsCookieJarTest {
    private lateinit var preferences: JsonKvStore
    private lateinit var storage: CommonsCookieStorage
    private lateinit var cookieJar: CommonsCookieJar

    @Before
    fun setUp() {
        preferences = mock()
        storage = CommonsCookieStorage(preferences)
        cookieJar = CommonsCookieJar(storage)
    }

    @Test
    fun `save stores a new cookie and persists the storage`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val cookie = cookie("session", "value", "commons.wikimedia.org")

        cookieJar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), storage["commons.wikimedia.org"])
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `save replaces a cookie with the same name`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val original = cookie("session", "old", "commons.wikimedia.org")
        val replacement = cookie("session", "new", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(original)

        cookieJar.saveFromResponse(url, listOf(replacement))

        assertEquals(listOf(replacement), storage["commons.wikimedia.org"])
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `save does not persist an identical cookie or an empty response`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val cookie = cookie("session", "value", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(cookie)

        cookieJar.saveFromResponse(url, listOf(cookie))
        cookieJar.saveFromResponse(url, emptyList())

        verify(preferences, never()).putString(any(), any())
    }

    @Test
    fun `save removes expired and deleted cookies by name`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val session = cookie("session", "value", "commons.wikimedia.org")
        val preference = cookie("preference", "value", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(session, preference)

        cookieJar.saveFromResponse(
            url,
            listOf(
                cookie("session", "ignored", "commons.wikimedia.org", expiresAt = 1L),
                cookie("preference", "deleted", "commons.wikimedia.org"),
            ),
        )

        assertEquals(emptyList<Cookie>(), storage["commons.wikimedia.org"])
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `load includes host cookies and only central auth cookies from commons`() {
        val centralAuth = cookie("centralauth_Session", "value", "commons.wikimedia.org")
        val commonsPreference = cookie("preference", "value", "commons.wikimedia.org")
        val wikipediaCookie = cookie("wiki_session", "value", "en.wikipedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(centralAuth, commonsPreference)
        storage["en.wikipedia.org"] = mutableListOf(wikipediaCookie)

        val loaded = cookieJar.loadForRequest("https://en.wikipedia.org".toHttpUrl())

        assertEquals(listOf(centralAuth, wikipediaCookie), loaded)
        verify(preferences, never()).putString(any(), any())
    }

    @Test
    fun `load excludes expired cookies`() {
        val expired = cookie("session", "old", "commons.wikimedia.org", expiresAt = 1L)
        val current = cookie("session", "new", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(expired, current)

        val loaded = cookieJar.loadForRequest("https://commons.wikimedia.org".toHttpUrl())

        assertEquals(listOf(current), loaded)
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `clear empties cookie storage and persists it`() {
        storage["commons.wikimedia.org"] =
            mutableListOf(cookie("session", "value", "commons.wikimedia.org"))

        cookieJar.clear()

        assertEquals(emptySet<String>(), storage.domains)
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `save defaults to URL host authority when cookie domain is empty`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val cookie = Cookie.parse(url, "session=value")!!

        cookieJar.saveFromResponse(url, listOf(cookie))

        assertEquals(listOf(cookie), storage["commons.wikimedia.org"])
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `load ignores cookies from unrelated domains`() {
        val unrelatedCookie = cookie("session", "value", "unrelated.org")
        storage["unrelated.org"] = mutableListOf(unrelatedCookie)

        val loaded = cookieJar.loadForRequest("https://commons.wikimedia.org".toHttpUrl())

        assertEquals(emptyList<Cookie>(), loaded)
        verify(preferences, never()).putString(any(), any())
    }

    @Test
    fun `save handles mix of unchanged and new cookies`() {
        val url = "https://commons.wikimedia.org".toHttpUrl()
        val existingCookie = cookie("session", "value", "commons.wikimedia.org")
        val newCookie = cookie("preference", "value2", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(existingCookie)

        cookieJar.saveFromResponse(url, listOf(existingCookie, newCookie))

        assertEquals(listOf(existingCookie, newCookie), storage["commons.wikimedia.org"])
        verify(preferences).putString(eq("cookie_store"), any())
    }

    @Test
    fun `load never leaks centralauth cookies to third-party https domains`() {
        val centralAuthToken = cookie("centralauth_Token", "secret_token", "commons.wikimedia.org")
        val centralAuthSession = cookie("centralauth_Session", "secret_session", "wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(centralAuthToken)
        storage["wikimedia.org"] = mutableListOf(centralAuthSession)

        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://attacker.example.com/collect".toHttpUrl()),
        )
        val campaignsUrl =
            "https://raw.githubusercontent.com/commons-app/campaigns/master/campaigns.json".toHttpUrl()
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest(campaignsUrl),
        )
    }

    @Test
    fun `load blocks suffix-spoofed domains lacking dot boundary`() {
        val centralAuth = cookie("centralauth_Token", "secret", "commons.wikimedia.org")
        val commonsSession = cookie("commonswikiSession", "sess", "commons.wikimedia.org")
        val wikimediaCookie = cookie("WMF-Last-Access", "today", "wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(centralAuth, commonsSession)
        storage["wikimedia.org"] = mutableListOf(wikimediaCookie)

        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://notwikimedia.org/w/api.php".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://fakewikipedia.org/w/api.php".toHttpUrl()),
        )
        // evilcommons.wikimedia.org must not receive commons.wikimedia.org local session cookie
        val subWikimedia = cookieJar.loadForRequest("https://evilcommons.wikimedia.org/".toHttpUrl())
        assertEquals(listOf(centralAuth, wikimediaCookie), subWikimedia)
    }

    @Test
    fun `load and save reject cleartext http urls`() {
        val centralAuth = cookie("centralauth_Token", "secret", "commons.wikimedia.org")
        val session = cookie("commonswikiSession", "sess", "commons.wikimedia.org")
        storage["commons.wikimedia.org"] = mutableListOf(centralAuth, session)

        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("http://commons.wikimedia.org/w/api.php".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("http://en.wikipedia.org/w/api.php".toHttpUrl()),
        )

        val injected = cookie("commonswikiSession", "hijacked", "commons.wikimedia.org")
        cookieJar.saveFromResponse("http://commons.wikimedia.org/w/api.php".toHttpUrl(), listOf(injected))
        assertEquals(listOf(centralAuth, session), storage["commons.wikimedia.org"])
    }

    @Test
    fun `load isolates production centralauth cookies from wmflabs toolforge and beta cluster`() {
        val prodToken = cookie("centralauth_Token", "prod_secret", "wikimedia.org")
        storage["wikimedia.org"] = mutableListOf(prodToken)

        // Multi-tenant community cloud hosts must never receive production CentralAuth cookies
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://tools.wmflabs.org/commons-android-app".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://some-tool.toolforge.org/".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://commons.wikimedia.beta.wmflabs.org/w/api.php".toHttpUrl()),
        )

        // Official Wikimedia SUL sister domains DO receive production CentralAuth cookies over HTTPS
        assertEquals(
            listOf(prodToken),
            cookieJar.loadForRequest("https://www.wikidata.org/w/api.php".toHttpUrl()),
        )
        assertEquals(
            listOf(prodToken),
            cookieJar.loadForRequest("https://meta.m.wikimedia.org/wiki/Special:Contact/accountvanishapps".toHttpUrl()),
        )
    }

    @Test
    fun `load shares beta cluster centralauth cookies only within matching beta cluster realm`() {
        val betaSession = cookie("centralauth_Session", "beta_sess", "commons.wikimedia.beta.wmflabs.org")
        storage["commons.wikimedia.beta.wmflabs.org"] = mutableListOf(betaSession)

        assertEquals(
            listOf(betaSession),
            cookieJar.loadForRequest("https://meta.m.wikimedia.beta.wmflabs.org/wiki/Main_Page".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://tools.wmflabs.org/commons-android-app".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://commons.wikimedia.org/w/api.php".toHttpUrl()),
        )
    }

    @Test
    fun `load enforces hostOnly and path matching and blocks cross-domain save injection`() {
        val commonsUrl = "https://commons.wikimedia.org/w/api.php".toHttpUrl()
        val hostOnlySession = Cookie.parse(commonsUrl, "commonswikiSession=host_only; Path=/w; Secure; HttpOnly")!!
        cookieJar.saveFromResponse(commonsUrl, listOf(hostOnlySession))

        assertEquals(
            listOf(hostOnlySession),
            cookieJar.loadForRequest("https://commons.wikimedia.org/w/api.php".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://commons.wikimedia.org/wiki/Main_Page".toHttpUrl()),
        )
        assertEquals(
            emptyList<Cookie>(),
            cookieJar.loadForRequest("https://sub.commons.wikimedia.org/w/api.php".toHttpUrl()),
        )

        // Unrelated origin must not be able to overwrite commons.wikimedia.org cookies
        val spoofed = cookie("commonswikiSession", "evil", "commons.wikimedia.org")
        cookieJar.saveFromResponse("https://evil.example.org/".toHttpUrl(), listOf(spoofed))
        assertEquals(listOf(hostOnlySession), storage["commons.wikimedia.org"])
    }

    private fun cookie(
        name: String,
        value: String,
        domain: String,
        expiresAt: Long = Long.MAX_VALUE,
    ): Cookie =
        Cookie
            .Builder()
            .name(name)
            .value(value)
            .domain(domain)
            .expiresAt(expiresAt)
            .build()
}
