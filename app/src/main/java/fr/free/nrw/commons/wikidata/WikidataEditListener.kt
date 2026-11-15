package fr.free.nrw.commons.wikidata

abstract class WikidataEditListener {
    var authenticationStateListener: WikidataP18EditListener? = null

    abstract fun onSuccessfulWikidataEdit()

    interface WikidataP18EditListener {
        fun onWikidataEditSuccessful()
    }
}
