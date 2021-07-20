package fr.free.nrw.commons.wikidata.model;

import java.util.List;

/**
 * Model class for API response obtained from search for depictions
 */
public class DepictSearchResponse {
    private final List<DepictSearchItem> search;

    /**
     * Constructor to initialise value of the search object
     */
    public DepictSearchResponse(List<DepictSearchItem> search) {
        this.search = search;
    }

    /**
     * @return List<DepictSearchItem> for the DepictSearchResponse
     */
    public List<DepictSearchItem> getSearch() {
        return search;
    }
}
