package fr.free.nrw.commons.media

import fr.free.nrw.commons.wikidata.mwapi.MwResponse

class MwParseResponse : MwResponse() {
    private var parse: MwParseResult? = null

    fun parse(): MwParseResult? = parse

    fun success(): Boolean = parse != null
}
