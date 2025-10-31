package fr.free.nrw.commons.nearby

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Handles the links to Wikipedia, Commons, and Wikidata that are displayed for a Place
 */
@Parcelize
class Sitelinks(
    private val wikipediaLink: String?,
    private val commonsLink: String?,
    private val wikidataLink: String?
) : Parcelable {
    /**
     * Gets the Wikipedia link for a Place
     * @return Wikipedia link
     */
    fun getWikipediaLink(): Uri? {
        return sanitiseString(wikipediaLink)
    }

    /**
     * Gets the Commons link for a Place
     * @return Commons link
     */
    fun getCommonsLink(): Uri? {
        return sanitiseString(commonsLink)
    }

    /**
     * Gets the Wikidata link for a Place
     * @return Wikidata link
     */
    fun getWikidataLink(): Uri? {
        return sanitiseString(wikidataLink)
    }

    override fun toString(): String {
        return "Sitelinks{" +
                "wikipediaLink='" + wikipediaLink + '\'' +
                ", commonsLink='" + commonsLink + '\'' +
                ", wikidataLink='" + wikidataLink + '\'' +
                '}'
    }

    /**
     * Builds a list of Sitelinks for a Place
     */
    class Builder {
        private var wikidataLink: String? = null
        private var commonsLink: String? = null
        private var wikipediaLink: String? = null

        fun setWikipediaLink(link: String): Builder {
            wikipediaLink = link
            return this
        }

        fun setWikidataLink(link: String): Builder {
            wikidataLink = link
            return this
        }

        fun setCommonsLink(link: String?): Builder {
            commonsLink = link
            return this
        }

        fun build(): Sitelinks {
            return Sitelinks(wikipediaLink, commonsLink, wikidataLink)
        }
    }

    companion object {
        /**
         * Sanitises and parses the links before using them
         * @param stringUrl unsanitised link
         * @return sanitised and parsed link
         */
        private fun sanitiseString(stringUrl: String?): Uri? {
            return stringUrl?.let {
                val sanitisedStringUrl = it.replace("[<>\n\r]".toRegex(), "").trim { it <= ' ' }
                Uri.parse(sanitisedStringUrl)
            }
        }
    }
}
