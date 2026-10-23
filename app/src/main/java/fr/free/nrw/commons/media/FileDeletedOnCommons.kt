package fr.free.nrw.commons.media

import fr.free.nrw.commons.wikidata.mwapi.MwIOException
import fr.free.nrw.commons.wikidata.mwapi.MwQueryPage

/**
 * Raised when a media query this screen already made says the file page is missing.
 */
class FileDeletedOnCommonsException : Exception()

/**
 * True when the query pages are present, none of them have image info, and at least one is
 * marked missing. An empty list is a different failure and is not a deletion.
 */
internal fun pagesIndicateDeletedFile(pages: List<MwQueryPage>): Boolean =
    pages.isNotEmpty() &&
        pages.none { it.imageInfo() != null } &&
        pages.any { it.missing() }

/**
 * True for the deleted-file signal from a request the app already made.
 * A plain network failure is not a deletion.
 */
fun Throwable?.isFileDeletedOnCommons(): Boolean {
    var current = this
    while (current != null) {
        if (current is FileDeletedOnCommonsException) {
            return true
        }
        if ((current as? MwIOException)?.error?.code == "missingtitle") {
            return true
        }
        current = current.cause
    }
    return false
}
