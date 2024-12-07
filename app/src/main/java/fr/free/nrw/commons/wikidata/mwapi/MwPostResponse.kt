package fr.free.nrw.commons.wikidata.mwapi

open class MwPostResponse : MwResponse() {
    val successVal: Int = 0

    fun success(result: String?): Boolean =
        "success" == result
}

