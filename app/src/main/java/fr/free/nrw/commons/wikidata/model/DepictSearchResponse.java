package fr.free.nrw.commons.wikidata.model;

import java.util.List;

/**
 * Model clsss for API response obtained from search for depictions
 */

public class DepictSearchResponse {
    private final List<DepictSearchItem> search;

    public DepictSearchResponse(List<DepictSearchItem> search) {
        this.search = search;
    }

    public List<DepictSearchItem> getSearch() {
        return search;
    }
}
