package fr.free.nrw.commons.actions

import org.wikipedia.dataclient.mwapi.MwResponse

/**
 * Response of the Thanks API.
 * Context:
 * The Commons Android app lets you thank other contributors who have uploaded a great picture.
 * See https://www.mediawiki.org/wiki/Extension:Thanks
 */
class MwThankPostResponse : MwResponse() {
    var result: Result? = null

    inner class Result {
        var success: Int? = null
        var recipient: String? = null
    }
}
