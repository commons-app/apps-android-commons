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
