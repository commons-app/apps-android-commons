package fr.free.nrw.commons.explore.map

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.media.MediaCollectionClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExploreMapCalls
    @Inject
    constructor() {
        @Inject
        @JvmField
        var mediaCollectionClient: MediaCollectionClient? = null

        /**
         * Calls method to query Commons for uploads around a location
         *
         * @param currentLatLng coordinates of search location
         * @return list of places obtained
         */
        fun callCommonsQuery(currentLatLng: LatLng): List<Media> {
            val coordinates = currentLatLng.latitude.toString() + "|" + currentLatLng.longitude
            return mediaCollectionClient!!.getMediaListFromGeoSearch(coordinates).blockingGet()
        }
    }
