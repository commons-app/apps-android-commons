package fr.free.nrw.commons.explore.map;

import fr.free.nrw.commons.location.LatLng;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.models.Media;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class ExploreMapCalls {

    @Inject
    MediaClient mediaClient;

    @Inject
    public ExploreMapCalls() {
    }

    /**
     * Calls method to query Commons for uploads around a location
     *
     * @param curLatLng coordinates of search location
     * @return list of places obtained
     */
    List<Media> callCommonsQuery(final LatLng curLatLng) {
        String coordinates = curLatLng.getLatitude() + "|" + curLatLng.getLongitude();
        return mediaClient.getMediaListFromGeoSearch(coordinates).blockingGet();
    }

}
