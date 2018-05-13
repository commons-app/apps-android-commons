package fr.free.nrw.commons.wikidata;

public class WikidataEditListenerImpl extends WikidataEditListener {

    public WikidataEditListenerImpl() {
    }

    @Override
    public void onSuccessfulWikidataEdit() {
        if (wikidataP18EditListener != null) {
            wikidataP18EditListener.onWikidataEditSuccessful();
        }
    }
}