package fr.free.nrw.commons.nearby;

import android.location.Location;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.nearby.model.NearbyQueryParams;
import java.io.IOException;
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

    private static final double INITIAL_RADIUS = 0.3; // in kilometers
    private static final double RADIUS_MULTIPLIER = 2.0;
    public double radius = INITIAL_RADIUS;

    private final OkHttpJsonApiClient okHttpJsonApiClient;

    /**
     * Reads Wikidata query to check nearby wikidata items which needs picture, with a circular
     * search. As a point is center of a circle with a radius will be set later.
     *
     * @param okHttpJsonApiClient
     */
    @Inject
    public NearbyPlaces(OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }

    /**
     * Expands the radius as needed for the Wikidata query
     *
     * @param currentLatLng           coordinates of search location
     * @param lang                user's language
     * @param returnClosestResult true if only the nearest point is desired
     * @param customQuery
     * @return list of places obtained
     */
    List<Place> radiusExpander(final LatLng currentLatLng, final String lang,
        final boolean returnClosestResult, @Nullable final String customQuery) throws Exception {

        final int minResults;
        final double maxRadius;

        List<Place> places = Collections.emptyList();

        // If returnClosestResult is true, then this means that we are trying to get closest point
        // to use in cardView in Contributions fragment
        if (returnClosestResult) {
            minResults = 1; // Return closest nearby place
            maxRadius = 5;  // Return places only in 5 km area
            radius = INITIAL_RADIUS; // refresh radius again, otherwise increased radius is grater than MAX_RADIUS, thus returns null
        } else {
            minResults = 20;
            maxRadius = 300.0; // in kilometers
            radius = INITIAL_RADIUS;
        }

        // Increase the radius gradually to find a satisfactory number of nearby places
        while (radius <= maxRadius) {
            places = getFromWikidataQuery(currentLatLng, lang, radius, customQuery);
            Timber.d("%d results at radius: %f", places.size(), radius);
            if (places.size() >= minResults) {
                break;
            }
            radius *= RADIUS_MULTIPLIER;
        }
        // make sure we will be able to send at least one request next time
        if (radius > maxRadius) {
            radius = maxRadius;
        }
        return places;
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     *
     * @param cur         coordinates of search location
     * @param lang        user's language
     * @param radius      radius for search, as determined by radiusExpander()
     * @param customQuery
     * @return list of places obtained
     * @throws IOException if query fails
     */
    public List<Place> getFromWikidataQuery(final LatLng cur, final String lang,
        final double radius,
        @Nullable final String customQuery) throws Exception {
        return okHttpJsonApiClient
            .getNearbyPlaces(cur, lang, radius, customQuery);
    }

    /**
     * Retrieves a list of places from a Wikidata query based on screen coordinates and optional
     * parameters.
     *
     * @param centerPoint             The center of the map, used for radius queries if required.
     * @param screenTopRight          The top right corner of the screen (latitude, longitude).
     * @param screenBottomLeft        The bottom left corner of the screen (latitude, longitude).
     * @param lang                    The language for the query.
     * @param shouldQueryForMonuments Flag indicating whether to include monuments in the query.
     * @param customQuery             Optional custom SPARQL query to use instead of default
     *                                queries.
     * @return A list of places obtained from the Wikidata query.
     * @throws Exception If an error occurs during the retrieval process.
     */
    public List<Place> getFromWikidataQuery(
        final fr.free.nrw.commons.location.LatLng centerPoint,
        final fr.free.nrw.commons.location.LatLng screenTopRight,
        final fr.free.nrw.commons.location.LatLng screenBottomLeft, final String lang,
        final boolean shouldQueryForMonuments,
        @Nullable final String customQuery) throws Exception {
        if (customQuery != null){
            return okHttpJsonApiClient
                .getNearbyPlaces(screenTopRight, screenBottomLeft, lang, shouldQueryForMonuments,
                    customQuery);
        }

        final int lowerLimit = 1000, upperLimit=1500;

        final float[] results = new float[1];
        Location.distanceBetween(centerPoint.getLatitude(), screenTopRight.getLongitude(),
            centerPoint.getLatitude(), screenBottomLeft.getLongitude(), results);
        final float longGap = results[0]/1000f;
        Location.distanceBetween(screenTopRight.getLatitude(), centerPoint.getLongitude(),
            screenBottomLeft.getLatitude(), centerPoint.getLongitude(), results);
        final float latGap = results[0]/1000f;

        if (Math.max(longGap,latGap)<100f){
            final int itemCount = okHttpJsonApiClient.getNearbyItemCount(
                new NearbyQueryParams.Rectangular(screenTopRight, screenBottomLeft));
            if(itemCount<upperLimit) {
                return okHttpJsonApiClient.getNearbyPlaces(screenTopRight, screenBottomLeft, lang,
                    shouldQueryForMonuments, null);
            }
        }

        int minRadius = 0, maxRadius = Math.round(Math.min(100f, Math.min(longGap, latGap)))*100;
        int targetRadius = maxRadius/2;
        while (minRadius<maxRadius) {
            targetRadius = minRadius + (maxRadius - minRadius + 1) / 2;
            final int itemCount = okHttpJsonApiClient.getNearbyItemCount(
                new NearbyQueryParams.Radial(centerPoint, targetRadius / 100f));
            if (itemCount >= lowerLimit && itemCount < upperLimit){
                break;
            }
            if (targetRadius>maxRadius/2 && itemCount<lowerLimit/5) {
                minRadius = targetRadius + (maxRadius - targetRadius + 1) / 2;
                continue;
            }
            if (itemCount<upperLimit) {
                minRadius = targetRadius;
            } else {
                 maxRadius = targetRadius - 1;
            }
        }
        return new java.util.ArrayList<Place>();
    }

    /**
     * Retrieves a list of places based on the provided list of places and language.
     *
     * This method fetches place information from a Wikidata query using the specified language.
     *
     * @param placeList A list of Place objects for which to fetch information.
     * @param lang      The language code to use for the query.
     * @return A list of Place objects obtained from the Wikidata query.
     * @throws Exception If an error occurs during the retrieval process.
     */
    public List<Place> getPlaces(final List<Place> placeList,
        final String lang) throws Exception {
        return okHttpJsonApiClient
            .getPlaces(placeList, lang);
    }

    /**
     * Runs the Wikidata query to retrieve the KML String
     *
     * @param leftLatLng  coordinates of Left Most position
     * @param rightLatLng coordinates of Right Most position
     * @throws IOException if query fails
     */
    public String getPlacesAsKML(LatLng leftLatLng, LatLng rightLatLng) throws Exception {
        return okHttpJsonApiClient.getPlacesAsKML(leftLatLng, rightLatLng);
    }

    /**
     * Runs the Wikidata query to retrieve the GPX String
     *
     * @param leftLatLng  coordinates of Left Most position
     * @param rightLatLng coordinates of Right Most position
     * @throws IOException if query fails
     */
    public String getPlacesAsGPX(LatLng leftLatLng, LatLng rightLatLng) throws Exception {
        return okHttpJsonApiClient.getPlacesAsGPX(leftLatLng, rightLatLng);
    }

}
