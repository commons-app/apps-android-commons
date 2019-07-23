package fr.free.nrw.commons.wikidata.model;


/**
 * Model class for Depiction item returned from API after calling searchForDepicts
 */

public class DepictSearchItem {
    private final String id;
    private final String pageid;
    private final String url;
    private final String label;
    private final String description;

    public DepictSearchItem(String id, String pageid, String url, String label, String description) {
        this.id = id;
        this.pageid = pageid;
        this.url = url;
        this.label = label;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getPageid() {
        return pageid;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }
}
