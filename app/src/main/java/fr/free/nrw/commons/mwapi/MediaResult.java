package fr.free.nrw.commons.mwapi;

public class MediaResult {
    private final String wikiSource;
    private final String parseTreeXmlSource;

    MediaResult(String wikiSource, String parseTreeXmlSource) {
        this.wikiSource = wikiSource;
        this.parseTreeXmlSource = parseTreeXmlSource;
    }

    public String getWikiSource() {
        return wikiSource;
    }

    public String getParseTreeXmlSource() {
        return parseTreeXmlSource;
    }
}
