package fr.free.nrw.commons.mwapi;

public class MediaResult {
    private final String wikiSource;
    private final String parseTreeXmlSource;

    /**
     * Full-fledged constructor of MediaResult
     *
     * @param wikiSource         Media wiki source
     * @param parseTreeXmlSource Media tree parsed in XML
     */
    MediaResult(String wikiSource, String parseTreeXmlSource) {
        this.wikiSource = wikiSource;
        this.parseTreeXmlSource = parseTreeXmlSource;
    }

    /**
     * Gets wiki source
     * @return Wiki source
     */
    public String getWikiSource() {
        return wikiSource;
    }

    /**
     * Gets tree parsed in XML
     * @return XML parsed tree
     */
    public String getParseTreeXmlSource() {
        return parseTreeXmlSource;
    }
}
