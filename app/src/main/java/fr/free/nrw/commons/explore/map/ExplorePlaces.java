package fr.free.nrw.commons.explore.map;

import android.os.Build.VERSION_CODES;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
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
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Singleton;
import timber.log.Timber;

@Singleton
public class ExplorePlaces {

    @Inject
    MediaClient mediaClient;

    @Inject
    public ExplorePlaces() {
    }

    /**
     * Expands the radius as needed for the Wikidata query
     *
     * @param curLatLng           coordinates of search location
     * @return list of places obtained
     */
    List<Media> callCommonsQuery(final LatLng curLatLng, int limit, boolean isFromSearchActivity, String query)
        throws Exception {
        Single<List<Media>> mediaListSingle;
        if (isFromSearchActivity) {
            return getFromCommonsWithQuery(curLatLng, query);
        } else {
            mediaListSingle =  getFromCommonsWithoutQuery(curLatLng, limit);
            return mediaListSingle.blockingGet();
        }
    }

    /**
     * Runs the Wikidata query to populate the Places around search location
     *
     * @param cur coordinates of search location
     * @return list of places obtained
     * @throws IOException if query fails
     */
    public Single<List<Media>> getFromCommonsWithoutQuery(final LatLng cur, final int limit) throws Exception {
        String coordinates = cur.getLatitude() + "|" + cur.getLongitude();
        return mediaClient.getMediaListFromGeoSearch(coordinates, limit);
    }

    public List<Media> getFromCommonsWithQuery(final LatLng cur, final String query) throws Exception {
        return mediaClient.getMediaListFromSearchWithLocation(query, 30, 0);
    }
}
