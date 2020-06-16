package fr.free.nrw.commons.wikidata;

/**
 * Listener for wikidata edits
 */
public class WikidataEditListenerImpl extends WikidataEditListener {

  public WikidataEditListenerImpl() {
  }

  /**
   * Fired when wikidata P18 edit is successful. If there's an active listener, then it is fired
   */
  @Override
  public void onSuccessfulWikidataEdit() {
    if (wikidataP18EditListener != null) {
      wikidataP18EditListener.onWikidataEditSuccessful();
    }
  }
}
