package fr.free.nrw.commons.nearby.presenter

import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.utils.LocationUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

private const val SCROLL_DELAY = 800L // Delay for debounce of onscroll, in milliseconds.
private const val LOCAL_SCROLL_DELAY = 200L // SCROLL_DELAY but for local db place search

/**
 * Presenter for NearbyParentFragment that coordinates map updates, location
 * changes, area searches, and map scrolling.
 */
class NearbyParentFragmentPresenter(
    bookmarkLocationDao: BookmarkLocationsDao,
    placesRepository: PlacesRepository,
    nearbyController: NearbyController,
    placeDetailsLoader: NearbyPlaceDetailsLoader? = null,
) : BaseNearbyParentFragmentPresenter(bookmarkLocationDao, placesRepository, nearbyController) {
    override val placeDetailsLoader: NearbyPlaceDetailsLoader =
        placeDetailsLoader ?: NearbyPlaceDetailsLoader(
            bookmarkLocationDao,
            placesRepository,
            nearbyController,
        ) { nearbyParentFragmentView }

    private var isNearbyLocked = false

    private var customQuery: String? = null

    private var placeSearchJob: Job? = null
    private var isSearchInProgress = false
    private var localPlaceSearchJob: Job? = null

    fun initializeMapOperations() {
        lockUnlockNearby(false)
        updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        nearbyParentFragmentView.setCheckBoxAction()
    }

    /**
     * Nearby updates takes time, since they are network operations. During update time, we don't
     * want to get any other calls from user. So locking nearby.
     *
     * @param isNearbyLocked true means lock, false means unlock
     */
    override fun lockUnlockNearby(isNearbyLocked: Boolean) {
        this.isNearbyLocked = isNearbyLocked
        if (isNearbyLocked) {
            nearbyParentFragmentView.disableFABRecenter()
        } else {
            nearbyParentFragmentView.enableFABRecenter()
        }
    }

    /**
     * This method should be the single point to update Map and List. Triggered by location changes
     *
     * @param locationChangeType defines if location changed significantly or slightly
     */
    override fun updateMapAndList(locationChangeType: LocationChangeType?) {
        Timber.d("Presenter updates map and list")
        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns")
            return
        }

        if (!nearbyParentFragmentView.isNetworkConnectionEstablished()) {
            Timber.d("Network connection is not established")
            return
        }

        val lastLocation = nearbyParentFragmentView.getLastMapFocus()
        currentLatLng =
            if (nearbyParentFragmentView.getMapCenter() != null) {
                nearbyParentFragmentView.getMapCenter()
            } else {
                lastLocation
            }
        if (currentLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable")
            return
        }
        // check if an upload is in progress
        if (nearbyController.isInternalUploadInProgress) {
            // reset the flag and exit early to prevent unnecessary fetchPlaces call
            nearbyController.isInternalUploadInProgress = false
            Timber.d("Internal upload in progress, skipping map update")
            return
        }

        if (isNearbyLocked) {
            Timber.d("Nearby is locked, so updateMapAndList returns")
            return
        }

        /**
         * Significant changed - Markers and current location will be updated together
         * Slightly changed - Only current position marker will be updated
         */
        if (locationChangeType == LocationChangeType.CUSTOM_QUERY) {
            Timber.d("ADVANCED_QUERY_SEARCH")
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            var updatedLocationByUser =
                LocationUtils.deriveUpdatedLocationFromSearchQuery(
                    customQuery!!,
                )
            if (updatedLocationByUser == null) {
                updatedLocationByUser = lastLocation
            }
            nearbyParentFragmentView.populatePlaces(updatedLocationByUser, customQuery)
        } else if (locationChangeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED ||
            locationChangeType == LocationChangeType.MAP_UPDATED
        ) {
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapCenter())
        } else if (locationChangeType == LocationChangeType.SEARCH_CUSTOM_AREA) {
            Timber.d("SEARCH_CUSTOM_AREA")
            lockUnlockNearby(true)
            nearbyParentFragmentView.setProgressBarVisibility(true)
            nearbyParentFragmentView.populatePlaces(nearbyParentFragmentView.getMapFocus())
        } else { // Means location changed slightly, ie user is walking or driving.
            Timber.d("Means location changed slightly")
        }
    }

    override fun onLocationChangedSignificantly(latLng: LatLng) {
        Timber.d("Location significantly changed")
        updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
    }

    override fun onLocationChangedSlightly(latLng: LatLng) {
        Timber.d("Location significantly changed")
        updateMapAndList(LocationChangeType.LOCATION_SLIGHTLY_CHANGED)
    }

    override fun onLocationChangedMedium(latLng: LatLng) {
        Timber.d("Location changed medium")
    }

    /**
     * Handles the map scroll user action for `NearbyParentFragment`
     *
     * @param scope The lifecycle scope of `nearbyParentFragment`'s `viewLifecycleOwner`
     * @param isNetworkAvailable Whether to load pins from the internet or from the cache.
     */
    @Override
    override fun handleMapScrolled(
        scope: LifecycleCoroutineScope?,
        isNetworkAvailable: Boolean,
    ) {
        scope ?: return

        placeSearchJob?.cancel()
        localPlaceSearchJob?.cancel()
        if (isNetworkAvailable) {
            placeSearchJob =
                scope.launch(Dispatchers.Main) {
                    delay(SCROLL_DELAY)
                    if (!isSearchInProgress) {
                        isSearchInProgress = true; // search executing flag
                        // Start Search
                        try {
                            searchInTheArea()
                        } finally {
                            isSearchInProgress = false
                        }
                    }
                }
        } else {
            placeDetailsLoader.loadPlacesDataAyncJob?.cancel()
            localPlaceSearchJob =
                scope.launch(Dispatchers.IO) {
                    delay(LOCAL_SCROLL_DELAY)

                    // retrieve and check map boundaries for nullability
                    val mapBottomLeft = nearbyParentFragmentView.screenBottomLeft
                    val mapTopRight = nearbyParentFragmentView.screenTopRight
                    if (mapBottomLeft == null || mapTopRight == null) {
                        Timber.d(
                            "Map boundaries (screenBottomLeft or screenTopRight) are null, " +
                                "skipping local place search as map state is not ready.",
                        )
                        return@launch // exits the coroutine gracefully, preventing the crash.
                    }

                    val mapFocus = nearbyParentFragmentView.mapFocus
                    val markerPlaceGroups =
                        placesRepository
                            .fetchPlaces(
                                nearbyParentFragmentView.screenBottomLeft,
                                nearbyParentFragmentView.screenTopRight,
                            ).sortedBy { it.getDistanceInDouble(mapFocus) }
                            .take(NearbyController.MAX_RESULTS)
                            .map {
                                MarkerPlaceGroup(
                                    bookmarkLocationDao.findBookmarkLocation(it.name),
                                    it,
                                )
                            }
                    ensureActive()
                    NearbyController.currentLocation = mapFocus
                    placeDetailsLoader.schedulePlacesUpdate(markerPlaceGroups, force = true)
                    withContext(Dispatchers.Main) {
                        nearbyParentFragmentView.updateSnackbar(!markerPlaceGroups.isEmpty())
                    }
                }
        }
    }

    override fun setAdvancedQuery(query: String) {
        this.customQuery = query
    }

    /**
     * Initiates a search for places within the area. Depending on whether the search
     * is close to the current location, the map and list are updated
     * accordingly.
     */
    fun searchInTheArea() {
        if (searchCloseToCurrentLocation()) {
            updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        } else {
            updateMapAndList(LocationChangeType.SEARCH_CUSTOM_AREA)
        }
    }

    fun onMapReady() {
        initializeMapOperations()
    }
}
