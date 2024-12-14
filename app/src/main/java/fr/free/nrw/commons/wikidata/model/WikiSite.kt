package fr.free.nrw.commons.wikidata.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.language.AppLanguageLookUpTable
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_CN_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_HK_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_MO_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_SG_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.CHINESE_TW_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.NORWEGIAN_BOKMAL_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.NORWEGIAN_LEGACY_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.SIMPLIFIED_CHINESE_LANGUAGE_CODE
import fr.free.nrw.commons.language.AppLanguageLookUpTable.Companion.TRADITIONAL_CHINESE_LANGUAGE_CODE
import org.apache.commons.lang3.StringUtils
import java.util.Locale

/**
 * The base URL and Wikipedia language code for a MediaWiki site. Examples:
 *
 *
 * <lh>Name: scheme / authority / language code</lh>
 *  * English Wikipedia: HTTPS / en.wikipedia.org / en
 *  * Chinese Wikipedia: HTTPS / zh.wikipedia.org / zh-hans or zh-hant
 *  * Meta-Wiki: HTTPS / meta.wikimedia.org / (none)
 *  * Test Wikipedia: HTTPS / test.wikipedia.org / test
 *  * Võro Wikipedia: HTTPS / fiu-vro.wikipedia.org / fiu-vro
 *  * Simple English Wikipedia: HTTPS / simple.wikipedia.org / simple
 *  * Simple English Wikipedia (beta cluster mirror): HTTP / simple.wikipedia.beta.wmflabs.org / simple
 *  * Development: HTTP / 192.168.1.11:8080 / (none)
 *
 *
 * **As shown above, the language code or mapping is part of the authority:**
 *
 * <lh>Validity: authority / language code</lh>
 *  * Correct: "test.wikipedia.org" / "test"
 *  * Correct: "wikipedia.org", ""
 *  * Correct: "no.wikipedia.org", "nb"
 *  * Incorrect: "wikipedia.org", "test"
 *
 */
class WikiSite : Parcelable {
    //TODO: remove @SerializedName. this is now in the TypeAdapter and a "uri" case may be added
    @SerializedName("domain")
    private val uri: Uri

    private var languageCode: String? = null

    constructor(uri: Uri) {
        val tempUri = ensureScheme(uri)
        var authority = tempUri.authority

        if (authority.isWikipedia && tempUri.path?.startsWith("/wiki") == true) {
            // Special case for Wikipedia only: assume English subdomain when none given.
            authority = "en.wikipedia.org"
        }

        val langVariant = getLanguageVariantFromUri(tempUri)
        languageCode = if (!TextUtils.isEmpty(langVariant)) {
            langVariant
        } else {
            authorityToLanguageCode(authority!!)
        }

        this.uri = Uri.Builder()
            .scheme(tempUri.scheme)
            .encodedAuthority(authority)
            .build()
    }

    private val String?.isWikipedia: Boolean get() =
        (this == "wikipedia.org" || this == "www.wikipedia.org")

    /** Get language variant code from a Uri, e.g. "zh-*", otherwise returns empty string.  */
    private fun getLanguageVariantFromUri(uri: Uri): String {
        if (TextUtils.isEmpty(uri.path)) {
            return ""
        }
        val parts = StringUtils.split(StringUtils.defaultString(uri.path), '/')
        return if (parts.size > 1 && parts[0] != "wiki") parts[0] else ""
    }

    constructor(url: String) : this(
        if (url.startsWith("http")) Uri.parse(url) else if (url.startsWith("//"))
            Uri.parse("$DEFAULT_SCHEME:$url")
        else
            Uri.parse("$DEFAULT_SCHEME://$url")
    )

    constructor(authority: String, languageCode: String) : this(authority) {
        this.languageCode = languageCode
    }

    fun scheme(): String =
        if (TextUtils.isEmpty(uri.scheme)) DEFAULT_SCHEME else uri.scheme!!

    /**
     * @return The complete wiki authority including language subdomain but not including scheme,
     * authentication, port, nor trailing slash.
     *
     * @see [URL syntax](https://en.wikipedia.org/wiki/Uniform_Resource_Locator.Syntax)
     */
    fun authority(): String = uri.authority!!

    /**
     * Like [.authority] but with a "m." between the language subdomain and the rest of the host.
     * Examples:
     *
     *
     *  * English Wikipedia: en.m.wikipedia.org
     *  * Chinese Wikipedia: zh.m.wikipedia.org
     *  * Meta-Wiki: meta.m.wikimedia.org
     *  * Test Wikipedia: test.m.wikipedia.org
     *  * Võro Wikipedia: fiu-vro.m.wikipedia.org
     *  * Simple English Wikipedia: simple.m.wikipedia.org
     *  * Simple English Wikipedia (beta cluster mirror): simple.m.wikipedia.beta.wmflabs.org
     *  * Development: m.192.168.1.11
     *
     */
    fun mobileAuthority(): String = authorityToMobile(authority())

    /**
     * Get wiki's mobile URL
     * Eg. https://en.m.wikipedia.org
     * @return
     */
    fun mobileUrl(): String = String.format("%1\$s://%2\$s", scheme(), mobileAuthority())

    fun subdomain(): String = languageCodeToSubdomain(languageCode!!)

    /**
     * @return A path without an authority for the segment including a leading "/".
     */
    fun path(segment: String): String = "/w/$segment"


    fun uri(): Uri = uri

    /**
     * @return The canonical URL. e.g., https://en.wikipedia.org.
     */
    fun url(): String = uri.toString()

    /**
     * @return The canonical URL for segment. e.g., https://en.wikipedia.org/w/foo.
     */
    fun url(segment: String): String = url() + path(segment)

    /**
     * @return The wiki language code which may differ from the language subdomain. Empty if
     * language code is unknown. Ex: "en", "zh-hans", ""
     *
     * @see AppLanguageLookUpTable
     */
    fun languageCode(): String = languageCode!!

    // Auto-generated
    override fun equals(o: Any?): Boolean {
        if (this === o) {
            return true
        }
        if (o == null || javaClass != o.javaClass) {
            return false
        }

        val wiki = o as WikiSite

        if (uri != wiki.uri) {
            return false
        }
        return languageCode == wiki.languageCode
    }

    // Auto-generated
    override fun hashCode(): Int {
        var result = uri.hashCode()
        result = 31 * result + languageCode.hashCode()
        return result
    }

    // Auto-generated
    override fun toString(): String {
        return ("WikiSite{"
                + "uri=" + uri
                + ", languageCode='" + languageCode + '\''
                + '}')
    }

    override fun describeContents(): Int = 0

    override fun writeToParcel(dest: Parcel, flags: Int) {
        dest.writeParcelable(uri, 0)
        dest.writeString(languageCode)
    }

    protected constructor(`in`: Parcel) {
        uri = `in`.readParcelable(Uri::class.java.classLoader)!!
        languageCode = `in`.readString()
    }

    /** @param authority Host and optional port.
     */
    private fun authorityToMobile(authority: String): String {
        if (authority.startsWith("m.") || authority.contains(".m.")) {
            return authority
        }
        return authority.replaceFirst(("^" + subdomain() + "\\.?").toRegex(), "$0m.")
    }

    companion object {
        const val WIKIPEDIA_URL = "https://wikipedia.org/"
        const val DEFAULT_SCHEME: String = "https"

        @JvmField
        val CREATOR: Parcelable.Creator<WikiSite> = object : Parcelable.Creator<WikiSite> {
            override fun createFromParcel(parcel: Parcel): WikiSite {
                return WikiSite(parcel)
            }

            override fun newArray(size: Int): Array<WikiSite?> {
                return arrayOfNulls(size)
            }
        }

        fun forDefaultLocaleLanguageCode(): WikiSite {
            val languageCode: String = Locale.getDefault().language
            val subdomain = if (languageCode.isEmpty()) "" else languageCodeToSubdomain(languageCode) + "."
            val uri = ensureScheme(Uri.parse(WIKIPEDIA_URL))
            return WikiSite(subdomain + uri.authority, languageCode)
        }

        private fun languageCodeToSubdomain(languageCode: String): String = when (languageCode) {
            SIMPLIFIED_CHINESE_LANGUAGE_CODE,
            TRADITIONAL_CHINESE_LANGUAGE_CODE,
            CHINESE_CN_LANGUAGE_CODE,
            CHINESE_HK_LANGUAGE_CODE,
            CHINESE_MO_LANGUAGE_CODE,
            CHINESE_SG_LANGUAGE_CODE,
            CHINESE_TW_LANGUAGE_CODE -> CHINESE_LANGUAGE_CODE

            NORWEGIAN_BOKMAL_LANGUAGE_CODE -> NORWEGIAN_LEGACY_LANGUAGE_CODE // T114042

            else -> languageCode
        }

        private fun authorityToLanguageCode(authority: String): String {
            val parts = authority.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val minLengthForSubdomain = 3
            if (parts.size < minLengthForSubdomain || parts.size == minLengthForSubdomain && parts[0] == "m") {
                // ""
                // wikipedia.org
                // m.wikipedia.org
                return ""
            }
            return parts[0]
        }

        private fun ensureScheme(uri: Uri): Uri {
            if (TextUtils.isEmpty(uri.scheme)) {
                return uri.buildUpon().scheme(DEFAULT_SCHEME).build()
            }
            return uri
        }
    }
}
