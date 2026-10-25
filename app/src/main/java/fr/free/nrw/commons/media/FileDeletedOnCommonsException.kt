package fr.free.nrw.commons.media

import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage

/**
 * Raised when a media query this screen already made says the file page is missing.
 */
class FileDeletedOnCommonsException : Exception()

/**
 * True when every page in the response is marked missing. An empty list is a different
 * failure and is not a deletion. This only reads the missing flag, which the query already
 * returned.
 */
internal fun pagesIndicateDeletedFile(pages: List<MwQueryPage>): Boolean =
    pages.isNotEmpty() && pages.all { it.missing() }

/**
 * True for the deleted-file signal from a request the app already made.
 * A plain network failure is not a deletion.
 */
fun Throwable?.isFileDeletedOnCommons(): Boolean {
    var current = this
    var deleted = false
    while (current != null && !deleted) {
        deleted =
            current is FileDeletedOnCommonsException ||
            (current as? MwIOException)?.error?.code == "missingtitle"
        current = current.cause
    }
    return deleted
}
