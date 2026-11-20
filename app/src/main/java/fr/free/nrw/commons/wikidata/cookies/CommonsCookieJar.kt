package fr.free.nrw.commons.wikidata.cookies

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import java.util.Locale

class CommonsCookieJar(
    private val cookieStorage: CommonsCookieStorage,
) : CookieJar {
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        if (!url.isHttps) {
            return emptyList()
        }

        val cookieList = mutableListOf<Cookie>()
        val directCookieNames = mutableSetOf<String>()

        cookieStorage.domains.forEach { domainSpec ->
            if (domainMatches(url.host, domainSpec)) {
                appendDirectCookies(cookieList, directCookieNames, domainSpec, url)
            } else if (canTransferCentralAuth(domainSpec, url)) {
                appendCentralAuthCookies(cookieList, directCookieNames, domainSpec)
            }
        }
        return cookieList
    }

    private fun appendDirectCookies(
        cookieList: MutableList<Cookie>,
        directCookieNames: MutableSet<String>,
        domainSpec: String,
        url: HttpUrl,
    ) {
        val matched = collectValidCookies(domainSpec, url = url, prefix = null)
        matched.forEach { cookie ->
            cookieList.removeAll { it.name == cookie.name && it.name !in directCookieNames }
            if (cookie.name !in directCookieNames) {
                cookieList.add(cookie)
                directCookieNames.add(cookie.name)
            }
        }
    }

    private fun appendCentralAuthCookies(
        cookieList: MutableList<Cookie>,
        directCookieNames: Set<String>,
        domainSpec: String,
    ) {
        val ssoCookies = collectValidCookies(domainSpec, url = null, prefix = CENTRALAUTH_PREFIX)
        ssoCookies.forEach { cookie ->
            if (cookie.name !in directCookieNames && cookieList.none { it.name == cookie.name }) {
                cookieList.add(cookie)
            }
        }
    }

    override fun saveFromResponse(
        url: HttpUrl,
        cookies: List<Cookie>,
    ) {
        if (!url.isHttps || cookies.isEmpty()) {
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
        val domainSpec = cookie.domain.ifEmpty { url.host }
        if (!domainMatches(url.host, domainSpec)) {
            return false
        }
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
            cookie.expiresAt < System.currentTimeMillis() || "deleted" == cookie.value ->
                cookieList.removeAll { it.name == cookie.name }
            cookie in cookieList -> false
            else -> {
                cookieList.removeAll { it.name == cookie.name }
                cookieList.add(cookie)
                true
            }
        }

    private fun collectValidCookies(
        domainSpec: String,
        url: HttpUrl?,
        prefix: String?,
    ): List<Cookie> {
        val inList = cookieStorage[domainSpec]
        val outList = mutableListOf<Cookie>()
        var cookieJarModified = false

        val iterator = inList.iterator()
        while (iterator.hasNext()) {
            val cookie = iterator.next()
            val matchesPrefix = prefix == null || cookie.name.startsWith(prefix)
            val matchesUrl = url == null || cookie.matches(url)
            if (cookie.expiresAt < System.currentTimeMillis()) {
                iterator.remove()
                cookieJarModified = true
            } else if (matchesPrefix && matchesUrl) {
                outList.add(cookie)
            }
        }

        if (cookieJarModified) {
            cookieStorage[domainSpec] = inList
            cookieStorage.save()
        }
        return outList
    }

    fun clear() {
        cookieStorage.clear()
    }

    companion object {
        private const val CENTRALAUTH_PREFIX = "centralauth_"
        private const val PROD_CENTRALAUTH_SOURCE = "wikimedia.org"
        private const val BETA_WMFLABS_DOMAIN = "beta.wmflabs.org"
        private const val BETA_WMCLOUD_DOMAIN = "beta.wmcloud.org"

        private val PROD_WIKIMEDIA_SSO_DOMAINS =
            listOf(
                "wikimedia.org",
                "wikipedia.org",
                "wikidata.org",
                "mediawiki.org",
                "wikibooks.org",
                "wikinews.org",
                "wikiquote.org",
                "wikisource.org",
                "wikiversity.org",
                "wikivoyage.org",
                "wiktionary.org",
            )

        private val BETA_WIKIMEDIA_SSO_DOMAINS =
            listOf(
                BETA_WMFLABS_DOMAIN,
                BETA_WMCLOUD_DOMAIN,
            )

        /**
         * RFC 6265 dot-boundary domain match (rejects suffix spoofing such as
         * `notwikimedia.org` or `evilcommons.wikimedia.org`).
         */
        fun domainMatches(
            host: String,
            domainSpec: String,
        ): Boolean {
            val normalizedHost = host.lowercase(Locale.ROOT).trimEnd('.')
            val normalizedDomain = domainSpec.lowercase(Locale.ROOT).trimStart('.').trimEnd('.')
            return normalizedHost.isNotEmpty() &&
                normalizedDomain.isNotEmpty() &&
                (normalizedHost == normalizedDomain || normalizedHost.endsWith(".$normalizedDomain"))
        }

        /**
         * Returns true only when [url] uses HTTPS and its host belongs to an official
         * Wikimedia CentralAuth SSO realm (production SUL projects or Beta Cluster).
         */
        fun isTrustedWikimediaSsoUrl(url: HttpUrl): Boolean =
            url.isHttps &&
                (
                    PROD_WIKIMEDIA_SSO_DOMAINS.any { domainMatches(url.host, it) } ||
                        BETA_WIKIMEDIA_SSO_DOMAINS.any { domainMatches(url.host, it) }
                )

        /**
         * Restricts cross-domain `centralauth_*` cookie transfer strictly within the matching
         * CentralAuth realm over HTTPS: production `*.wikimedia.org` cookies only transfer to
         * production Wikimedia SUL projects (never to multi-tenant `wmflabs.org` / `wmcloud.org`
         * or third-party hosts), and Beta Cluster cookies stay within their Beta Cluster domain.
         */
        internal fun canTransferCentralAuth(
            sourceDomainSpec: String,
            targetUrl: HttpUrl,
        ): Boolean =
            when {
                !targetUrl.isHttps -> false
                domainMatches(sourceDomainSpec, PROD_CENTRALAUTH_SOURCE) ->
                    PROD_WIKIMEDIA_SSO_DOMAINS.any { domainMatches(targetUrl.host, it) }
                domainMatches(sourceDomainSpec, BETA_WMFLABS_DOMAIN) ->
                    domainMatches(targetUrl.host, BETA_WMFLABS_DOMAIN)
                domainMatches(sourceDomainSpec, BETA_WMCLOUD_DOMAIN) ->
                    domainMatches(targetUrl.host, BETA_WMCLOUD_DOMAIN)
                else -> false
            }
    }
}
