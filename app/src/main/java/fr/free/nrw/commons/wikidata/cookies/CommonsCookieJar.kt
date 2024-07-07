package fr.free.nrw.commons.wikidata.cookies

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class CommonsCookieJar(private val cookieStorage: CommonsCookieStorage) : CookieJar {
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

    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) {
            return
        }

        var cookieJarModified = false
        cookies.forEach { cookie ->
            // Default to the URI's domain if cookie's domain is not explicitly set
            val domainSpec = cookie.domainSpec(url)
            if (!cookieStorage.contains(domainSpec)) {
                cookieStorage[domainSpec] = mutableListOf()
            }

            val cookieList = cookieStorage[domainSpec]
            if (cookie.expiredOrDeleted()) {
                cookieJarModified = cookieList.removeAll { it.name == cookie.name }
            } else {
                val i = cookieList.iterator()
                var exists = false
                while (i.hasNext()) {
                    val c = i.next()
                    if (c == cookie) {
                        // an identical cookie already exists, so we don't need to update it.
                        exists = true
                        break
                    } else if (c.name == cookie.name) {
                        // it's a cookie with the same name, but different contents, so remove the
                        // current cookie, so that the new one will be added.
                        i.remove()
                    }
                }
                if (!exists) {
                    cookieList.add(cookie)
                    cookieJarModified = true
                }
            }
            cookieStorage[domainSpec] = cookieList
        }

        if (cookieJarModified) {
            cookieStorage.save()
        }
    }

    private fun buildCookieList(
        outList: MutableList<Cookie>, inList: MutableList<Cookie>, prefix: String?
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

    private fun Cookie.expiredOrDeleted(): Boolean =
        expiresAt < System.currentTimeMillis() || "deleted" == value

    private fun Cookie.domainSpec(url: HttpUrl): String =
        domain.ifEmpty { url.toUri().getAuthority() }

    fun clear() {
        cookieStorage.clear()
    }

}
