package fr.free.nrw.commons.wikidata.cookies

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CommonsCookieJar(
    private val cookieStorage: CommonsCookieStorage,
) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val cookieList = mutableListOf<Cookie>()
        val domain: String = url.toUri().getAuthority()

        cookieStorage.domains.forEach { domainSpec ->
            if (domain.endsWith(domainSpec, true)) {
                buildCookieList(cookieList, cookieStorage[domainSpec], null)
            } else if (domainSpec.endsWith("commons.wikimedia.org")) {
                // For sites outside the wikipedia.org domain, transfer the centralauth cookies
                // from commons.wikimedia.org unconditionally.
                buildCookieList(cookieList, cookieStorage[domainSpec], "centralauth_")
            }
        }
        return cookieList
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        if (cookies.isEmpty()) {
            return
        }

        var cookieJarModified = false
        for (cookie in cookies) {
            if (saveCookie(url, cookie)) {
                cookieJarModified = true
            }
        }

        if (cookieJarModified) {
            cookieStorage.save()
        }
    }

    private fun saveCookie(
        url: HttpUrl,
        cookie: Cookie,
    ): Boolean {
        val domainSpec = cookie.domainSpec(url)
        val cookieList = cookieStorage[domainSpec]
        val modified = updateCookieList(cookieList, cookie)
        if (modified) {
            cookieStorage[domainSpec] = cookieList
        }
        return modified
    }

    private fun updateCookieList(
        cookieList: MutableList<Cookie>,
        cookie: Cookie,
    ): Boolean =
        when {
            cookie.expiredOrDeleted() -> cookieList.removeAll { it.name == cookie.name }
            cookie in cookieList -> false
            else -> {
                cookieList.removeAll { it.name == cookie.name }
                cookieList.add(cookie)
                true
            }
        }

    private fun buildCookieList(
        outList: MutableList<Cookie>,
        inList: MutableList<Cookie>,
        prefix: String?,
    ) {
        var cookieJarModified = false

        val i = inList.iterator()
        while (i.hasNext()) {
            val cookie = i.next()
            if (prefix != null && !cookie.name.startsWith(prefix)) {
                continue
            }
            // But wait, is the cookie expired?
            if (cookie.expiresAt < System.currentTimeMillis()) {
                i.remove()
                cookieJarModified = true
            } else {
                outList.add(cookie)
            }
        }

        if (cookieJarModified) {
            cookieStorage.save()
        }
    }

    private fun Cookie.expiredOrDeleted(): Boolean = expiresAt < System.currentTimeMillis() || "deleted" == value

    private fun Cookie.domainSpec(url: HttpUrl): String = domain.ifEmpty { url.toUri().getAuthority() }

    fun clear() {
        cookieStorage.clear()
    }
}
