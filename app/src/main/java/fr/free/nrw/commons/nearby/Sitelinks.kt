package fr.free.nrw.commons.nearby

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import androidx.core.net.toUri

/**
 * Handles the links to Wikipedia, Commons, and Wikidata that are displayed for a Place
 */
@Parcelize
data class Sitelinks(
    val wikipediaLink: String? = null,
    val commonsLink: String? = null,
    val wikidataLink: String? = null
) : Parcelable {
    val wikipediaUri: Uri?
        get() = sanitiseString(wikipediaLink)

    val commonsUri: Uri?
        get() = sanitiseString(commonsLink)

    val wikidataUri: Uri?
        get() = sanitiseString(wikidataLink)

    companion object {
        /**
         * Sanitises and parses the links before using them
         * @param stringUrl unsanitised link
         * @return sanitised and parsed link
         */
        private fun sanitiseString(stringUrl: String?): Uri? {
            return stringUrl?.replace(
                "[<>\n\r]".toRegex(), ""
            )?.trim()?.toUri()
        }
    }
}
