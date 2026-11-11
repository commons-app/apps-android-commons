package fr.free.nrw.commons.nearby.presenter

import android.location.Location
import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.TimeZone
import java.util.concurrent.TimeUnit

private const val SEARCH_RADIUS_METERS = 2000.0
private const val SEARCH_RADIUS_THRESHOLD_FRACTION = 0.75
private const val CACHED_PLACE_MAX_DAYS = 5L

// Cached places older than this are removed from the cache once a day
private val CACHED_PLACE_MAX_AGE_MS = TimeUnit.DAYS.toMillis(CACHED_PLACE_MAX_DAYS)

// Key of the day when old cached places were last removed
private const val LAST_CACHED_PLACES_REMOVAL_DAY = "last_cached_places_removal_day"

/**
 * Handles place bookmark operations, cache expiration, pin click events, and marker updates
 * for NearbyParentFragment.
 */
class NearbyBookmarkCacheHandler(
    private val bookmarkLocationDao: BookmarkLocationsDao,
    private val placesRepository: PlacesRepository,
    val placeDetailsLoader: NearbyPlaceDetailsLoader,
    private val viewProvider: () -> NearbyParentFragmentContract.View,
    var lockNearby: ((Boolean) -> Unit)? = null,
    var updateMapAndList: ((LocationChangeType?) -> Unit)? = null,
) : NearbyPlaceOperations {
    /**
     * Handles the user action of toggling the bookmarked status of a given place. Updates the
     * bookmark status in the database, updates the UI to reflect the new state.
     *
     * @param place The place whose bookmarked status is to be toggled. If the place is `null`,
     *              the operation is skipped.
     */
    override fun toggleBookmarkedStatus(
        place: Place?,
        scope: LifecycleCoroutineScope?,
    ) {
        if (place == null) return
        var nowBookmarked: Boolean
        scope?.launch {
            nowBookmarked = bookmarkLocationDao.updateBookmarkLocation(place)
            placeDetailsLoader.bookmarkChangedPlaces.add(place)
            val placeIndex =
                NearbyController.markerLabelList.indexOfFirst { it.place.location == place.location }
            NearbyController.markerLabelList[placeIndex] =
                MarkerPlaceGroup(
                    nowBookmarked,
                    NearbyController.markerLabelList[placeIndex].place,
                )
            viewProvider().setFilterState()
        }
    }

    override fun onWikidataEditSuccessful() {
        updateMapAndList?.invoke(LocationChangeType.MAP_UPDATED)
    }

    /**
     * used to tell the asynchronous place detail loading job that a pin was clicked
     * so as to prevent it from turning grey on the next pin detail update
     *
     * @param place the place whose details have already been loaded because clicked pin
     */
    override fun handlePinClicked(place: Place) {
        placeDetailsLoader.handlePinClicked(place)
    }

    /**
     * Load the places' details from cache and Wikidata query, and update these details
     * into markerLabelList on UI
     *
     * @param places The list of places around the current location to load details for.
     */
    override fun loadPlacesDataAsync(
        places: List<MarkerPlaceGroup>,
        scope: LifecycleCoroutineScope?,
    ) {
        placeDetailsLoader.loadPlacesDataAsync(places, scope)
    }

    /**
     * Update places on the map, and asynchronously load their details from cache and Wikidata query
     *
     * @param nearbyPlaces This variable has the list of placecs
     * @param scope the lifecycle scope of `nearbyParentFragment`'s `viewLifecycleOwner`
     */
    override fun updateMapMarkers(
        nearbyPlaces: List<Place>?,
        currentLatLng: LatLng,
        scope: LifecycleCoroutineScope?,
    ) {
        val nearbyPlaceGroups =
            nearbyPlaces
                ?.sortedBy { it.getDistanceInDouble(currentLatLng) }
                ?.take(NearbyController.MAX_RESULTS)
                ?.map {
                    // currently only the place's location is known but bookmarks are stored by name
                    MarkerPlaceGroup(
                        false,
                        it,
                    )
                }
                ?: return

        lockNearby?.invoke(false) // So that new location updates wont come
        viewProvider().setProgressBarVisibility(false)
        loadPlacesDataAsync(nearbyPlaceGroups, scope)
    }

    /**
     * Removes the places cached more than 5 days ago, at most once a day, so that the cache
     * does not keep growing with places that are not shown anymore.
     *
     * @param applicationKvStore Stores the day of the last removal.
     */
    override fun removeOldCachedPlaces(applicationKvStore: JsonKvStore) {
        val now = System.currentTimeMillis()
        // Count days in the phone's time zone, so that a new day starts at local midnight
        val today = TimeUnit.MILLISECONDS.toDays(now + TimeZone.getDefault().getOffset(now))
        if (applicationKvStore.getLong(LAST_CACHED_PLACES_REMOVAL_DAY, -1L) == today) {
            return
        }
        placesRepository
            .removePlacesCachedBefore(now - CACHED_PLACE_MAX_AGE_MS)
            .subscribe(
                { applicationKvStore.putLong(LAST_CACHED_PLACES_REMOVAL_DAY, today) },
                { throwable -> Timber.e(throwable) },
            )
    }

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    override fun searchCloseToCurrentLocation(): Boolean {
        val view = viewProvider()
        if (null == view.getLastMapFocus()) {
            return true
        }
        val myLocation = Location("")
        val destLocation = Location("")
        destLocation.latitude = view.getMapFocus().latitude
        destLocation.longitude = view.getMapFocus().longitude
        myLocation.latitude = view.getLastMapFocus().latitude
        myLocation.longitude = view.getLastMapFocus().longitude
        val distance = myLocation.distanceTo(destLocation)

        return (distance <= SEARCH_RADIUS_METERS * SEARCH_RADIUS_THRESHOLD_FRACTION)
    }
}
