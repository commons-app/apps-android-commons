package fr.free.nrw.commons.wikidata

/**
 * Listener for wikidata edits
 */
class WikidataEditListenerImpl : WikidataEditListener() {
    /**
     * Fired when wikidata P18 edit is successful. If there's an active listener, then it is fired
     */
    override fun onSuccessfulWikidataEdit() {
        authenticationStateListener?.onWikidataEditSuccessful()
    }
}
