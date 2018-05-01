package fr.free.nrw.commons.category;

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

