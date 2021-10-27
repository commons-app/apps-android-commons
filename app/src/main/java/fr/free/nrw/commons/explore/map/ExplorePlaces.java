package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import javax.inject.Inject;

public class ExplorePlaces {
    private static final double INITIAL_RADIUS = 0.3; // in kilometers
    private static final double RADIUS_MULTIPLIER = 2.0;
    public double radius = INITIAL_RADIUS;

    private final OkHttpJsonApiClient okHttpJsonApiClient;

    /**
     * Reads Wikidata query to check nearby wikidata items which needs picture, with a circular
     * search. As a point is center of a circle with a radius will be set later.
     * @param okHttpJsonApiClient
     */
    @Inject
    public ExplorePlaces(OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }
}
