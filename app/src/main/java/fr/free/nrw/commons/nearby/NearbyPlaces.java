package fr.free.nrw.commons.nearby;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import timber.log.Timber;

/**
 * Handles the Wikidata query to obtain Places around search location
 */
@Singleton
public class NearbyPlaces {

    private static final double INITIAL_RADIUS = 1.0; // in kilometers
    private static final double RADIUS_MULTIPLIER = 1.618;
    public double radius = INITIAL_RADIUS;

    private final OkHttpJsonApiClient okHttpJsonApiClient;

    /**
     * Reads Wikidata query to check nearby wikidata items which needs picture, with a circular
     * search. As a point is center of a circle with a radius will be set later.
     * @param okHttpJsonApiClient
     */
    @Inject
    public NearbyPlaces(OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }

    /**
     * Expands the radius as needed for the Wikidata query
     * @param curLatLng coordinates of search location
     * @param lang user's language
     * @param returnClosestResult true if only the nearest point is desired
     * @return list of places obtained
     * @throws IOException if query fails
     */
    List<Place> radiusExpander(LatLng curLatLng, String lang, boolean returnClosestResult) throws IOException {

        int minResults;
        double maxRadius;

        List<Place> places = Collections.emptyList();

        // If returnClosestResult is true, then this means that we are trying to get closest point
        // to use in cardView in Contributions fragment
        if (returnClosestResult) {
            minResults = 1; // Return closest nearby place
            maxRadius = 5;  // Return places only in 5 km area
            radius = INITIAL_RADIUS; // refresh radius again, otherwise increased radius is grater than MAX_RADIUS, thus returns null
        } else {
            minResults = 40;
            maxRadius = 300.0; // in kilometers
            radius = INITIAL_RADIUS;
        }

            // Increase the radius gradually to find a satisfactory number of nearby places
            while (radius <= maxRadius) {
                try {
                    places = getFromWikidataQuery(curLatLng, lang, radius);
                } catch (InterruptedIOException e) {
                    Timber.e(e, "exception in fetching nearby places");
                    return places;
                }
                Timber.d("%d results at radius: %f", places.size(), radius);
                if (places.size() >= minResults) {
                    break;
                } else {
                    radius *= RADIUS_MULTIPLIER;
                }
            }
        // make sure we will be able to send at least one request next time
        if (radius > maxRadius) {
            radius = maxRadius;
        }
        return places;
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     * @param cur coordinates of search location
     * @param lang user's language
     * @param radius radius for search, as determined by radiusExpander()
     * @return list of places obtained
     * @throws IOException if query fails
     */
    public List<Place> getFromWikidataQuery(LatLng cur, String lang, double radius) throws IOException {
        return okHttpJsonApiClient.getNearbyPlaces(cur, lang, radius).blockingSingle();
    }
}
