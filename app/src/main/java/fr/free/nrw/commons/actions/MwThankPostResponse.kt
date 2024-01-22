package fr.free.nrw.commons.actions

import org.wikipedia.dataclient.mwapi.MwResponse

class MwThankPostResponse : MwResponse() {
    var result: Result? = null

    inner class Result {
        var success: Int? = null
        var recipient: String? = null
    }
}
