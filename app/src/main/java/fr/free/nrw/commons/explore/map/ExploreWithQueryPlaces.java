package fr.free.nrw.commons.explore.map;

import android.util.Log;
import androidx.annotation.Nullable;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.nearby.Place;
import fr.free.nrw.commons.utils.LengthUtils;
import io.reactivex.Single;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class ExploreWithQueryPlaces {

    private static final int MAX_LIMIT = 10000;
    private static final int LIMIT_MULTIPLIER = 2;

    @Inject
    MediaClient mediaClient;

    @Inject
    public ExploreWithQueryPlaces() {
    }

    /**
     * Expands the radius as needed for the Wikidata query
     *
     * @param curLatLng           coordinates of search location
     * @return list of places obtained
     */
    List<Media> callCommonsQuery(final LatLng curLatLng, int limit)
        throws Exception {

        double farthestDistance = 0;

        Single<List<Media>> mediaList = null;


        // Increase the radius gradually to find a satisfactory number of nearby places
        //while (limit <= MAX_LIMIT) {
        mediaList = getFromCommonsQueryWithSearch(curLatLng, limit);
        Timber.d("%d results at limit: %d", mediaList.blockingGet().size(), limit);
        //if (mediaList.blockingGet().size() >= minResults) {
        //  break;
        //}
        //limit *= LIMIT_MULTIPLIER;
        //}
        // make sure we will be able to send at least one request next time

        ExploreMapController.farthestDistance = farthestDistance;
        return mediaList.blockingGet();
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     *
     * @param cur                     coordinates of search location
     * @return list of places obtained
     * @throws IOException if query fails
     */
    public Single<List<Media>> getFromCommonsQueryWithSearch(final LatLng cur, final int limit) throws Exception {
        Log.d("nesli","lat long is"+ cur.getLatitude() + "-" + cur.getLongitude());
        String coordinates = cur.getLatitude() + "|" + cur.getLongitude();
        // TODO search key falan eklenmeli
        //return mediaClient.getMediaListFromSearchWithLocation(coordinates, limit);
        return null;
    }
}
