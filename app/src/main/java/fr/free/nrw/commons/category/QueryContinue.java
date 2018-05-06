package fr.free.nrw.commons.category;

/**
 * For APIs that return paginated responses, MediaWiki APIs uses the QueryContinue to facilitate fetching of subsequent pages
 * https://www.mediawiki.org/wiki/API:Raw_query_continue
 */
public class QueryContinue {
    private String continueParam;
    private String gcmContinueParam;

    public QueryContinue(String continueParam, String gcmContinueParam) {
        this.continueParam = continueParam;
        this.gcmContinueParam = gcmContinueParam;
    }

    public String getGcmContinueParam() {
        return gcmContinueParam;
    }

    public String getContinueParam() {
        return continueParam;
    }
}

