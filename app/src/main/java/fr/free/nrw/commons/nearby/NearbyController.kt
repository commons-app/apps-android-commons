package fr.free.nrw.commons.nearby

import androidx.annotation.MainThread
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.MapController
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.utils.LengthUtils.computeDistanceBetween
import fr.free.nrw.commons.utils.LengthUtils.formatDistanceBetween
import timber.log.Timber
import java.util.Collections
import java.util.Locale
import javax.inject.Inject
import kotlin.math.cos
import kotlin.math.min

class NearbyController @Inject constructor(
    private val nearbyPlaces: NearbyPlaces
) : MapController() {
    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng       current location for user
     * @param searchLatLng        the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @param customQuery         if this search is done via an advanced query
     * @return NearbyPlacesInfo a variable holds Place list without distance information and
     * boundary coordinates of current Place List
     */
    @Throws(Exception::class)
    fun loadAttractionsFromLocation(
        currentLatLng: LatLng?,
        searchLatLng: LatLng?,
        returnClosestResult: Boolean, checkingAroundCurrentLocation: Boolean,
        customQuery: String?
    ): NearbyPlacesInfo? {
        Timber.d("Loading attractions near %s", searchLatLng)
        val nearbyPlacesInfo = NearbyPlacesInfo()
        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but currentLatLng is null")
            return null
        }
        val places = nearbyPlaces
            .radiusExpander(
                searchLatLng, Locale.getDefault().getLanguage(), returnClosestResult,
                customQuery
            )

        if (null != places && places.size > 0) {
            val boundaryCoordinates = arrayOf<LatLng>(
                places.get(0).location!!,  // south
                places.get(0).location!!,  // north
                places.get(0).location!!,  // west
                places.get(0).location!!
            ) // east, init with a random location

            if (currentLatLng != null) {
                Timber.d("Sorting places by distance...")
                val distances: MutableMap<Place, Double> = mutableMapOf()
                for (place in places) {
                    distances.put(place, computeDistanceBetween(place.location!!, currentLatLng))
                    // Find boundaries with basic find max approach
                    if (place.location!!.latitude < boundaryCoordinates[0].latitude) {
                        boundaryCoordinates[0] = place.location!!
                    }
                    if (place.location!!.latitude > boundaryCoordinates[1].latitude) {
                        boundaryCoordinates[1] = place.location!!
                    }
                    if (place.location!!.longitude < boundaryCoordinates[2].longitude) {
                        boundaryCoordinates[2] = place.location!!
                    }
                    if (place.location!!.longitude > boundaryCoordinates[3].longitude) {
                        boundaryCoordinates[3] = place.location!!
                    }
                }
                Collections.sort(
                    places,
                    Comparator { lhs: Place?, rhs: Place? ->
                        val lhsDistance: Double = distances[lhs]!!
                        val rhsDistance: Double = distances[rhs]!!
                        (lhsDistance - rhsDistance).toInt()
                    }
                )
            }
            nearbyPlacesInfo.currentLatLng = currentLatLng
            nearbyPlacesInfo.searchLatLng = searchLatLng
            nearbyPlacesInfo.placeList = places
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates

            // Returning closes result means we use the controller for nearby card. So no need to set search this area flags
            if (!returnClosestResult) {
                // To remember latest search either around user or any point on map
                latestSearchLocation = searchLatLng
                latestSearchRadius = nearbyPlaces.radius * 1000 // to meter

                // Our radius searched around us, will be used to understand when user search their own location, we will follow them
                if (checkingAroundCurrentLocation) {
                    currentLocationSearchRadius = nearbyPlaces.radius * 1000 // to meter
                    currentLocation = currentLatLng
                }
            }
        }
        return nearbyPlacesInfo
    }

    @Throws(Exception::class)
    fun getPlacesAsKML(currentLocation: LatLng): String? {
        return nearbyPlaces.getPlacesAsKML(
            calculateSouthWest(currentLocation.latitude, currentLocation.longitude, 10.0),
            calculateNorthEast(currentLocation.latitude, currentLocation.longitude, 10.0)
        )
    }

    @Throws(Exception::class)
    fun getPlacesAsGPX(currentLocation: LatLng): String? {
        return nearbyPlaces.getPlacesAsGPX(
            calculateSouthWest(currentLocation.latitude, currentLocation.longitude, 10.0),
            calculateNorthEast(currentLocation.latitude, currentLocation.longitude, 10.0)
        )
    }

    /**
     * Retrieves a list of places based on the provided list of places and language.
     *
     * @param placeList A list of Place objects for which to fetch information.
     * @return A list of Place objects obtained from the Wikidata query.
     * @throws Exception If an error occurs during the retrieval process.
     */
    @Throws(Exception::class)
    fun getPlaces(placeList: List<Place>?): List<Place>? {
        return nearbyPlaces.getPlaces(placeList, Locale.getDefault().getLanguage())
    }

    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng                 The current latitude and longitude.
     * @param screenTopRight                The top right corner of the screen (latitude,
     * longitude).
     * @param screenBottomLeft              The bottom left corner of the screen (latitude,
     * longitude).
     * @param searchLatLng                  The latitude and longitude of the search location.
     * @param returnClosestResult           Flag indicating whether to return the closest result.
     * @param checkingAroundCurrentLocation Flag indicating whether to check around the current
     * location.
     * @param shouldQueryForMonuments       Flag indicating whether to include monuments in the
     * query.
     * @param customQuery                   Optional custom SPARQL query to use instead of default
     * queries.
     * @return An object containing information about nearby places.
     * @throws Exception If an error occurs during the retrieval process.
     */
    @Throws(Exception::class)
    fun loadAttractionsFromLocation(
        currentLatLng: LatLng,
        screenTopRight: LatLng,
        screenBottomLeft: LatLng,
        searchLatLng: LatLng,
        returnClosestResult: Boolean, checkingAroundCurrentLocation: Boolean,
        shouldQueryForMonuments: Boolean, customQuery: String?
    ): NearbyPlacesInfo? {
        Timber.d("Loading attractions near %s", searchLatLng)
        val nearbyPlacesInfo = NearbyPlacesInfo()

        if (searchLatLng == null) {
            Timber.d("Loading attractions nearby, but currentLatLng is null")
            return null
        }

        val places = nearbyPlaces.getFromWikidataQuery(
            currentLatLng, screenTopRight,
            screenBottomLeft, Locale.getDefault().getLanguage(), shouldQueryForMonuments,
            customQuery
        )

        if (null != places && places.size > 0) {
            val boundaryCoordinates = arrayOf<LatLng>(
                places.get(0).location!!,  // south
                places.get(0).location!!,  // north
                places.get(0).location!!,  // west
                places.get(0).location!!
            ) // east, init with a random location

            if (currentLatLng != null) {
                Timber.d("Sorting places by distance...")
                val distances: MutableMap<Place, Double> = mutableMapOf()
                for (place in places) {
                    distances.put(place, computeDistanceBetween(place.location!!, currentLatLng))
                    // Find boundaries with basic find max approach
                    if (place.location!!.latitude < boundaryCoordinates[0].latitude) {
                        boundaryCoordinates[0] = place.location!!
                    }
                    if (place.location!!.latitude > boundaryCoordinates[1].latitude) {
                        boundaryCoordinates[1] = place.location!!
                    }
                    if (place.location!!.longitude < boundaryCoordinates[2].longitude) {
                        boundaryCoordinates[2] = place.location!!
                    }
                    if (place.location!!.longitude > boundaryCoordinates[3].longitude) {
                        boundaryCoordinates[3] = place.location!!
                    }
                }
                Collections.sort<Place?>(
                    places,
                    Comparator { lhs: Place?, rhs: Place? ->
                        val lhsDistance: Double = distances[lhs]!!
                        val rhsDistance: Double = distances[rhs]!!
                        (lhsDistance - rhsDistance).toInt()
                    }
                )
            }
            nearbyPlacesInfo.currentLatLng = currentLatLng
            nearbyPlacesInfo.searchLatLng = searchLatLng
            nearbyPlacesInfo.placeList = places
            nearbyPlacesInfo.boundaryCoordinates = boundaryCoordinates

            // Returning closes result means we use the controller for nearby card. So no need to set search this area flags
            if (!returnClosestResult) {
                // To remember latest search either around user or any point on map
                latestSearchLocation = searchLatLng
                latestSearchRadius = nearbyPlaces.radius * 1000 // to meter

                // Our radius searched around us, will be used to understand when user search their own location, we will follow them
                if (checkingAroundCurrentLocation) {
                    currentLocationSearchRadius = nearbyPlaces.radius * 1000 // to meter
                    currentLocation = currentLatLng
                }
            }
        }
        return nearbyPlacesInfo
    }

    /**
     * Prepares Place list to make their distance information update later.
     *
     * @param currentLatLng       current location for user
     * @param searchLatLng        the location user wants to search around
     * @param returnClosestResult if this search is done to find closest result or all results
     * @return NearbyPlacesInfo a variable holds Place list without distance information and
     * boundary coordinates of current Place List
     */
    @Throws(Exception::class)
    fun loadAttractionsFromLocation(
        currentLatLng: LatLng?,
        searchLatLng: LatLng?,
        returnClosestResult: Boolean, checkingAroundCurrentLocation: Boolean
    ): NearbyPlacesInfo? {
        return loadAttractionsFromLocation(
            currentLatLng, searchLatLng, returnClosestResult,
            checkingAroundCurrentLocation, null
        )
    }

    companion object {
        const val MAX_RESULTS: Int = 1000
        var currentLocationSearchRadius: Double = 10.0 //in kilometers
        var currentLocation: LatLng? = null // Users latest fetched location
        var latestSearchLocation: LatLng? =
            null // Can be current and camera target on search this area button is used
        var latestSearchRadius: Double = 10.0 // Any last search radius except closest result search

        var markerLabelList: MutableList<MarkerPlaceGroup> = mutableListOf()

        fun calculateNorthEast(latitude: Double, longitude: Double, distance: Double): LatLng {
            val lat1 = Math.toRadians(latitude)
            val deltaLat = distance * 0.008
            val deltaLon = distance / cos(lat1) * 0.008
            val lat2 = latitude + deltaLat
            val lon2 = longitude + deltaLon

            return LatLng(lat2, lon2, 0f)
        }

        fun calculateSouthWest(latitude: Double, longitude: Double, distance: Double): LatLng {
            val lat1 = Math.toRadians(latitude)
            val deltaLat = distance * 0.008
            val deltaLon = distance / cos(lat1) * 0.008
            val lat2 = latitude - deltaLat
            val lon2 = longitude - deltaLon

            return LatLng(lat2, lon2, 0f)
        }

        /**
         * Loads attractions from location for map view, we need to return BaseMarkerOption data type.
         *
         * @param currentLatLng users current location
         * @param placeList     list of nearby places in Place data type
         * @return BaseMarkerOptions list that holds nearby places
         */
        fun loadAttractionsFromLocationToBaseMarkerOptions(
            currentLatLng: LatLng?,
            placeList: List<Place>?
        ): MutableList<BaseMarker> {
            var placeList = placeList
            val baseMarkersList: MutableList<BaseMarker> = mutableListOf()

            if (placeList == null) {
                return baseMarkersList
            }
            placeList = placeList.subList(0, min(placeList.size, MAX_RESULTS))
            for (place in placeList) {
                val baseMarker = BaseMarker()
                val distance = formatDistanceBetween(currentLatLng, place.location)
                place.distance = distance
                baseMarker.title = place.name!!
                baseMarker.position = LatLng(
                    place.location!!.latitude,
                    place.location!!.longitude, 0f
                )
                baseMarker.place = place
                baseMarkersList.add(baseMarker)
            }
            return baseMarkersList
        }


        /**
         * Updates makerLabelList item isBookmarked value
         *
         * @param place        place which is bookmarked
         * @param isBookmarked true is bookmarked, false if bookmark removed
         */
        @MainThread
        fun updateMarkerLabelListBookmark(place: Place, isBookmarked: Boolean) {
            val iter: MutableListIterator<MarkerPlaceGroup> = markerLabelList.listIterator()
            while (iter.hasNext()) {
                val markerPlaceGroup = iter.next()
                if (markerPlaceGroup.place.wikiDataEntityId == place.wikiDataEntityId) {
                    iter.set(MarkerPlaceGroup(isBookmarked, place))
                }
            }
        }
    }
}
