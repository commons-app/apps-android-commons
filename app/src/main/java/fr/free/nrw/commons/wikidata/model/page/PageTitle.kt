package fr.free.nrw.commons.wikidata.model.page

import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.google.gson.annotations.SerializedName
import fr.free.nrw.commons.wikidata.model.WikiSite
import timber.log.Timber
import java.io.UnsupportedEncodingException
import java.net.URLDecoder
import java.net.URLEncoder
import java.text.Normalizer
import java.util.Arrays
import java.util.Locale

/**
 * Represents certain vital information about a page, including the title, namespace,
 * and fragment (section anchor target).  It can also contain a thumbnail URL for the
 * page, and a short description retrieved from Wikidata.
 *
 * WARNING: This class is not immutable! Specifically, the thumbnail URL and the Wikidata
 * description can be altered after construction. Therefore do NOT rely on all the fields
 * of a PageTitle to remain constant for the lifetime of the object.
 */
class PageTitle : Parcelable {
    /**
     * The localised namespace of the page as a string, or null if the page is in mainspace.
     *
     * This field contains the prefix of the page's title, as opposed to the namespace ID used by
     * MediaWiki. Therefore, mainspace pages always have a null namespace, as they have no prefix,
     * and the namespace of a page will depend on the language of the wiki the user is currently
     * looking at.
     *
     * Examples:
     * * \[\[Manchester\]\] on enwiki will have a namespace of null
     * * \[\[Deutschland\]\] on dewiki will have a namespace of null
     * * [[User:Deskana]] on enwiki will have a namespace of "User"
     * * [[Utilisateur:Deskana]] on frwiki will have a namespace of "Utilisateur", even if you got
     * to the page by going to [[User:Deskana]] and having MediaWiki automatically redirect you.
     */
    // TODO: remove. This legacy code is the localized namespace name (File, Special, Talk, etc) but
    //       isn't consistent across titles. e.g., articles with colons, such as RTÉ News: Six One,
    //       are broken.
    private val namespace: String?
    private val text: String
    val fragment: String?
    var thumbUrl: String?

    @SerializedName("site")
    val wikiSite: WikiSite
    var description: String? = null
    private val properties: PageProperties?

    // TODO: remove after the restbase endpoint supports ZH variants.
    private var convertedText: String? = null

    constructor(namespace: String?, text: String, fragment: String?, thumbUrl: String?, wiki: WikiSite) {
        this.namespace = namespace
        this.text = text
        this.fragment = fragment
        this.thumbUrl = thumbUrl
        wikiSite = wiki
        properties = null
    }

    constructor(text: String?, wiki: WikiSite, thumbUrl: String?, description: String?, properties: PageProperties?) : this(text, wiki, thumbUrl, properties) {
        this.description = description
    }

    constructor(text: String?, wiki: WikiSite, thumbUrl: String?, description: String?) : this(text, wiki, thumbUrl) {
        this.description = description
    }

    constructor(namespace: String?, text: String, wiki: WikiSite) : this(namespace, text, null, null, wiki)

    @JvmOverloads
    constructor(text: String?, wiki: WikiSite, thumbUrl: String? = null) : this(text, wiki, thumbUrl, null as PageProperties?)

    private constructor(input: String?, wiki: WikiSite, thumbUrl: String?, properties: PageProperties?) {
        var text = input ?: ""
        // FIXME: Does not handle mainspace articles with a colon in the title well at all
        val fragParts = text.split("#".toRegex()).toTypedArray()
        text = fragParts[0]
        fragment = if (fragParts.size > 1) {
            decodeURL(fragParts[1]).replace(" ", "_")
        } else {
            null
        }

        val parts = text.split(":".toRegex()).toTypedArray()
        if (parts.size > 1) {
            val namespaceOrLanguage = parts[0]
            if (Arrays.asList(*Locale.getISOLanguages()).contains(namespaceOrLanguage)) {
                namespace = null
                wikiSite = WikiSite(wiki.authority(), namespaceOrLanguage)
            } else {
                wikiSite = wiki
                namespace = namespaceOrLanguage
            }
            this.text = TextUtils.join(":", Arrays.copyOfRange(parts, 1, parts.size))
        } else {
            wikiSite = wiki
            namespace = null
            this.text = parts[0]
        }

        this.thumbUrl = thumbUrl
        this.properties = properties
    }

    /**
     * Decodes a URL-encoded string into its UTF-8 equivalent. If the string cannot be decoded, the
     * original string is returned.
     * @param url The URL-encoded string that you wish to decode.
     * @return The decoded string, or the input string if the decoding failed.
     */
    private fun decodeURL(url: String): String {
        try {
            return URLDecoder.decode(url, "UTF-8")
        } catch (e: IllegalArgumentException) {
            // Swallow IllegalArgumentException (can happen with malformed encoding), and just
            // return the original string.
            Timber.d("URL decoding failed. String was: %s", url)
            return url
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    private fun getTextWithoutSpaces(): String =
        text.replace(" ", "_")

    fun getConvertedText(): String =
        if (convertedText == null) prefixedText else convertedText!!

    fun setConvertedText(convertedText: String?) {
        this.convertedText = convertedText
    }

    val displayText: String
        get() = prefixedText.replace("_", " ")

    val displayTextWithoutNamespace: String
        get() = text.replace("_", " ")

    fun hasProperties(): Boolean =
        properties != null

    val isMainPage: Boolean
        get() = properties != null && properties.isMainPage

    val isDisambiguationPage: Boolean
        get() = properties != null && properties.isDisambiguationPage

    val canonicalUri: String
        get() = getUriForDomain(wikiSite.authority())

    val mobileUri: String
        get() = getUriForDomain(wikiSite.mobileAuthority())

    fun getUriForAction(action: String?): String {
        try {
            return String.format(
                "%1\$s://%2\$s/w/index.php?title=%3\$s&action=%4\$s",
                wikiSite.scheme(),
                wikiSite.authority(),
                URLEncoder.encode(prefixedText, "utf-8"),
                action
            )
        } catch (e: UnsupportedEncodingException) {
            throw RuntimeException(e)
        }
    }

    // TODO: find a better way to check if the namespace is a ISO Alpha2 Code (two digits country code)
    val prefixedText: String
        get() = namespace?.let { addUnderscores(it) + ":" + getTextWithoutSpaces() }
            ?: getTextWithoutSpaces()

    private fun addUnderscores(text: String): String =
        text.replace(" ", "_")

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(namespace)
        parcel.writeString(text)
        parcel.writeString(fragment)
        parcel.writeParcelable(wikiSite, flags)
        parcel.writeParcelable(properties, flags)
        parcel.writeString(thumbUrl)
        parcel.writeString(description)
        parcel.writeString(convertedText)
    }

    override fun equals(o: Any?): Boolean {
        if (o !is PageTitle) {
            return false
        }

        val other = o
        // Not using namespace directly since that can be null
        return normalizedEquals(other.prefixedText, prefixedText) && other.wikiSite.equals(wikiSite)
    }

    // Compare two strings based on their normalized form, using the Unicode Normalization Form C.
    // This should be used when comparing or verifying strings that will be exchanged between
    // different platforms (iOS, desktop, etc) that may encode strings using inconsistent
    // composition, especially for accents, diacritics, etc.
    private fun normalizedEquals(str1: String?, str2: String?): Boolean {
        if (str1 == null || str2 == null) {
            return (str1 == null && str2 == null)
        }
        return (Normalizer.normalize(str1, Normalizer.Form.NFC)
                == Normalizer.normalize(str2, Normalizer.Form.NFC))
    }

    override fun hashCode(): Int {
        var result = prefixedText.hashCode()
        result = 31 * result + wikiSite.hashCode()
        return result
    }

    override fun toString(): String =
        prefixedText

    override fun describeContents(): Int = 0

    private fun getUriForDomain(domain: String): String = try {
        String.format(
            "%1\$s://%2\$s/wiki/%3\$s%4\$s",
            wikiSite.scheme(),
            domain,
            URLEncoder.encode(prefixedText, "utf-8"),
            if ((fragment != null && fragment.length > 0)) ("#$fragment") else ""
        )
    } catch (e: UnsupportedEncodingException) {
        throw RuntimeException(e)
    }

    private constructor(parcel: Parcel) {
        namespace = parcel.readString()
        text = parcel.readString()!!
        fragment = parcel.readString()
        wikiSite = parcel.readParcelable(WikiSite::class.java.classLoader)!!
        properties = parcel.readParcelable(PageProperties::class.java.classLoader)
        thumbUrl = parcel.readString()
        description = parcel.readString()
        convertedText = parcel.readString()
    }

    companion object {
        @JvmField
        val CREATOR: Parcelable.Creator<PageTitle> = object : Parcelable.Creator<PageTitle> {
            override fun createFromParcel(parcel: Parcel): PageTitle {
                return PageTitle(parcel)
            }

            override fun newArray(size: Int): Array<PageTitle?> {
                return arrayOfNulls(size)
            }
        }

        /**
         * Creates a new PageTitle object.
         * Use this if you want to pass in a fragment portion separately from the title.
         *
         * @param prefixedText title of the page with optional namespace prefix
         * @param fragment optional fragment portion
         * @param wiki the wiki site the page belongs to
         * @return a new PageTitle object matching the given input parameters
         */
        fun withSeparateFragment(
            prefixedText: String,
            fragment: String?, wiki: WikiSite
        ): PageTitle {
            return if (TextUtils.isEmpty(fragment)) {
                PageTitle(prefixedText, wiki, null, null as PageProperties?)
            } else {
                // TODO: this class needs some refactoring to allow passing in a fragment
                // without having to do string manipulations.
                PageTitle("$prefixedText#$fragment", wiki, null, null as PageProperties?)
            }
        }
    }
}
