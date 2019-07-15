package fr.free.nrw.commons.wikidata.model;

import java.util.List;

public class DepictSearchResponse {
    private final List<DepictSearchItem> search;

    public DepictSearchResponse(List<DepictSearchItem> search) {
        this.search = search;
    }

    public List<DepictSearchItem> getSearch() {
        return search;
    }
}
