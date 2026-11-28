package fr.free.nrw.commons.wikidata.model

import android.net.Uri
import android.os.Parcel
import fr.free.nrw.commons.TestCommonsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [21], application = TestCommonsApplication::class)
class WikiSiteTest {
    @Test
    fun authorityWithoutSchemeGetsDefaultHttpsScheme() {
        val site = WikiSite("en.wikipedia.org")
        assertEquals("https", site.scheme())
        assertEquals("en.wikipedia.org", site.authority())
        assertEquals("https://en.wikipedia.org", site.url())
    }

    @Test
    fun schemeRelativeAuthorityGetsDefaultScheme() {
        val site = WikiSite("//en.wikipedia.org")
        assertEquals("https", site.scheme())
        assertEquals("en.wikipedia.org", site.authority())
    }

    @Test
    fun explicitSchemeIsKept() {
        val site = WikiSite("http://test.wikipedia.org")
        assertEquals("http", site.scheme())
        assertEquals("test.wikipedia.org", site.authority())
    }

    @Test
    fun languageCodeComesFromFirstSubdomain() {
        assertEquals("en", WikiSite("en.wikipedia.org").languageCode())
        assertEquals("commons", WikiSite("commons.wikimedia.org").languageCode())
        assertEquals("fiu-vro", WikiSite("fiu-vro.wikipedia.org").languageCode())
    }

    @Test
    fun languageCodeIsEmptyWithoutSubdomain() {
        assertEquals("", WikiSite("wikipedia.org").languageCode())
    }

    @Test
    fun languageCodeIsEmptyForMobileSubdomain() {
        assertEquals("", WikiSite("m.wikipedia.org").languageCode())
    }

    @Test
    fun languageCodeIsExtractedBeforeMobileSubdomain() {
        assertEquals("en", WikiSite("en.m.wikipedia.org").languageCode())
    }

    @Test
    fun bareWikipediaWikiPathAssumesEnglish() {
        val site = WikiSite("https://wikipedia.org/wiki/Test")
        assertEquals("en.wikipedia.org", site.authority())
        assertEquals("en", site.languageCode())
    }

    @Test
    fun wwwWikipediaWikiPathAssumesEnglish() {
        val site = WikiSite("https://www.wikipedia.org/wiki/Test")
        assertEquals("en.wikipedia.org", site.authority())
        assertEquals("en", site.languageCode())
    }

    @Test
    fun pathIsDroppedFromCanonicalUrl() {
        val site = WikiSite("https://en.wikipedia.org/wiki/Test")
        assertEquals("https://en.wikipedia.org", site.url())
    }

    @Test
    fun languageVariantComesFromPathInsteadOfSubdomain() {
        val site = WikiSite("https://zh.wikipedia.org/zh-hant/Test")
        assertEquals("zh-hant", site.languageCode())
        assertEquals("zh.wikipedia.org", site.authority())
        assertEquals("https://zh.m.wikipedia.org", site.mobileUrl())
    }

    @Test
    fun wikiPathDoesNotCountAsLanguageVariant() {
        val site = WikiSite("https://zh.wikipedia.org/wiki/Test")
        assertEquals("zh", site.languageCode())
    }

    @Test
    fun indexScriptPathKeepsLanguageAndMobileUrl() {
        val site = WikiSite("https://en.wikipedia.org/w/index.php?title=Sandbox")
        assertEquals("en", site.languageCode())
        assertEquals("en", site.subdomain())
        assertEquals("en.m.wikipedia.org", site.mobileAuthority())
        assertEquals("https://en.m.wikipedia.org", site.mobileUrl())
    }

    @Test
    fun apiScriptPathIdentifiesTheSameWikiAsTheBaseUrl() {
        val canonicalSite = WikiSite("https://en.wikipedia.org")
        val apiSite = WikiSite("https://en.wikipedia.org/w/api.php?action=query")
        assertEquals(canonicalSite, apiSite)
        assertEquals(canonicalSite.hashCode(), apiSite.hashCode())
    }

    @Test
    fun twoArgumentConstructorOverridesLanguageCode() {
        val site = WikiSite("wikipedia.org", "test")
        assertEquals("test", site.languageCode())
        assertEquals("wikipedia.org", site.authority())
    }

    @Test
    fun subdomainMapsChineseVariantsToZh() {
        assertEquals("zh", WikiSite("zh.wikipedia.org", "zh-hans").subdomain())
        assertEquals("zh", WikiSite("zh.wikipedia.org", "zh-tw").subdomain())
    }

    @Test
    fun subdomainMapsBokmalToLegacyNo() {
        assertEquals("no", WikiSite("no.wikipedia.org").subdomain())
    }

    @Test
    fun subdomainDefaultsToLanguageCode() {
        assertEquals("en", WikiSite("en.wikipedia.org").subdomain())
    }

    @Test
    fun mobileAuthorityInsertsMobileSubdomain() {
        assertEquals("en.m.wikipedia.org", WikiSite("en.wikipedia.org").mobileAuthority())
        assertEquals("meta.m.wikimedia.org", WikiSite("meta.wikimedia.org").mobileAuthority())
    }

    @Test
    fun mobileAuthorityIsUnchangedWhenAlreadyMobile() {
        assertEquals("en.m.wikipedia.org", WikiSite("en.m.wikipedia.org").mobileAuthority())
    }

    @Test
    fun mobileUrlContainsMobileAuthority() {
        assertEquals("https://en.m.wikipedia.org", WikiSite("en.wikipedia.org").mobileUrl())
    }

    @Test
    fun pathAndUrlSegmentAreBuiltFromCanonicalUrl() {
        val site = WikiSite("en.wikipedia.org")
        assertEquals("/w/api.php", site.path("api.php"))
        assertEquals("https://en.wikipedia.org/w/api.php", site.url("api.php"))
    }

    @Test
    fun uriConstructorAddsMissingScheme() {
        val site = WikiSite(Uri.parse("//en.wikipedia.org"))
        assertEquals("https", site.scheme())
        assertEquals("en.wikipedia.org", site.authority())
    }

    @Test
    fun forDefaultLocaleLanguageCodeUsesLocaleLanguage() {
        val previous = Locale.getDefault()
        try {
            Locale.setDefault(Locale.FRENCH)
            assertEquals(WikiSite("fr.wikipedia.org"), WikiSite.forDefaultLocaleLanguageCode())
        } finally {
            Locale.setDefault(previous)
        }
    }

    @Test
    fun equalityRequiresSameUriAndLanguageCode() {
        assertEquals(WikiSite("en.wikipedia.org"), WikiSite("https://en.wikipedia.org"))
        assertEquals(WikiSite("en.wikipedia.org").hashCode(), WikiSite("https://en.wikipedia.org").hashCode())
        assertNotEquals(WikiSite("en.wikipedia.org"), WikiSite("fr.wikipedia.org"))
        assertNotEquals(WikiSite("wikipedia.org", "en"), WikiSite("wikipedia.org", "fr"))
    }

    @Test
    fun parcelRoundTripPreservesUriAndLanguageCode() {
        val site = WikiSite("en.wikipedia.org")
        val parcel = Parcel.obtain()
        try {
            site.writeToParcel(parcel, 0)
            parcel.setDataPosition(0)
            val restored = WikiSite.CREATOR.createFromParcel(parcel)
            assertEquals(site, restored)
            assertEquals("en", restored.languageCode())
        } finally {
            parcel.recycle()
        }
    }
}
