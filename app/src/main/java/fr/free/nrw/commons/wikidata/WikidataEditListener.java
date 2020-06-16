package fr.free.nrw.commons.wikidata;

public abstract class WikidataEditListener {

  protected WikidataP18EditListener wikidataP18EditListener;

  public abstract void onSuccessfulWikidataEdit();

  public void setAuthenticationStateListener(WikidataP18EditListener wikidataP18EditListener) {
    this.wikidataP18EditListener = wikidataP18EditListener;
  }

  public interface WikidataP18EditListener {

    void onWikidataEditSuccessful();
  }
}
