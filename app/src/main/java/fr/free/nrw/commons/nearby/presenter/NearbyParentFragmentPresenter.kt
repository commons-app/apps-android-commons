package fr.free.nrw.commons.nearby.presenter

import android.location.Location
import android.view.View
import androidx.annotation.MainThread
import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.BaseMarker
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.location.LocationServiceManager.LocationChangeType
import fr.free.nrw.commons.location.LocationUpdateListener
import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.utils.LocationUtils
import fr.free.nrw.commons.wikidata.WikidataConstants.PLACE_OBJECT
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.concurrent.CopyOnWriteArrayList

class NearbyParentFragmentPresenter
    (
    val bookmarkLocationDao: BookmarkLocationsDao,
    val placesRepository: PlacesRepository,
    val nearbyController: NearbyController
) :
    NearbyParentFragmentContract.UserActions,
    WikidataP18EditListener, LocationUpdateListener {

    private var isNearbyLocked = false
    private var currentLatLng: LatLng? = null

    private var placesLoadedOnce = false

    private var customQuery: String? = null

    private var nearbyParentFragmentView: NearbyParentFragmentContract.View = DUMMY

    private val clickedPlaces = CopyOnWriteArrayList<Place>()

    /**
     * used to tell the asynchronous place detail loading job that a pin was clicked
     * so as to prevent it from turning grey on the next pin detail update
     *
     * @param place the place whose details have already been loaded because clicked pin
     */
    fun handlePinClicked(place: Place) {
        clickedPlaces.add(place)
    }

    // the currently running job for async loading of pin details, cancelled when new pins are come
    private var loadPlacesDataAyncJob: Job? = null

    /**
     * - **batchSize**: number of places to fetch details of in a single request
     * - **connnectionCount**: number of parallel requests
     */
    private object LoadPlacesAsyncOptions {
        const val BATCH_SIZE = 3
        const val CONNECTION_COUNT = 3
    }

    private var schedulePlacesUpdateJob: Job? = null

    /**
     * - **skippedCount**: stores the number of updates skipped
     * - **skipLimit**: maximum number of consecutive updates that can be skipped
     * - **skipDelayMs**: The delay (in milliseconds) to wait for a new update.
     *
     * @see schedulePlacesUpdate
     */
    private object SchedulePlacesUpdateOptions {
        var skippedCount = 0
        const val SKIP_LIMIT = 3
        const val SKIP_DELAY_MS = 500L
    }

    // used to tell the asynchronous place detail loading job that the places' bookmarked status
    // changed so as to prevent inconsistencies
    private var bookmarkChangedPlaces = CopyOnWriteArrayList<Place>()

    /**
     * Schedules a UI update for the provided list of `MarkerPlaceGroup` objects. Since, the update
     * is performed on the main thread, it waits for a `SchedulePlacesUpdateOptions.skipDelayMs`
     * to see if a new update comes, and if one does, it discards the scheduled UI update.
     *
     * @param markerPlaceGroups The new list of `MarkerPlaceGroup` objects. If the list is empty, no
     *                          update will be performed.
     *
     * @see SchedulePlacesUpdateOptions
     */
    private suspend fun schedulePlacesUpdate(markerPlaceGroups: List<MarkerPlaceGroup>) =
        withContext(Dispatchers.Main) {
            if (markerPlaceGroups.isEmpty()) return@withContext
            schedulePlacesUpdateJob?.cancel()
            schedulePlacesUpdateJob = launch {
                if (SchedulePlacesUpdateOptions.skippedCount++
                    < SchedulePlacesUpdateOptions.SKIP_LIMIT
                ) {
                    delay(SchedulePlacesUpdateOptions.SKIP_DELAY_MS)
                }
                SchedulePlacesUpdateOptions.skippedCount = 0
                updatePlaceGroupsToControllerAndRender(markerPlaceGroups)
            }
        }

    /**
     * Handles the user action of toggling the bookmarked status of a given place. Updates the
     * bookmark status in the database, updates the UI to reflect the new state.
     *
     * @param place The place whose bookmarked status is to be toggled. If the place is `null`,
     *              the operation is skipped.
     */
    override fun toggleBookmarkedStatus(place: Place?) {
        if (place == null) return
        val nowBookmarked = bookmarkLocationDao.updateBookmarkLocation(place)
        bookmarkChangedPlaces.add(place)
        val placeIndex =
            NearbyController.markerLabelList.indexOfFirst { it.place.location == place.location }
        NearbyController.markerLabelList[placeIndex] = MarkerPlaceGroup(
            nowBookmarked,
            NearbyController.markerLabelList[placeIndex].place
        )
        nearbyParentFragmentView.setFilterState()
    }

    override fun attachView(view: NearbyParentFragmentContract.View) {
        this.nearbyParentFragmentView = view
    }

    override fun detachView() {
        this.nearbyParentFragmentView = DUMMY
    }

    override fun removeNearbyPreferences(applicationKvStore: JsonKvStore) {
        Timber.d("Remove place objects")
        applicationKvStore.remove(PLACE_OBJECT)
    }

    fun initializeMapOperations() {
        lockUnlockNearby(false)
        updateMapAndList(LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED)
        nearbyParentFragmentView.setCheckBoxAction()
    }

    /**
     * Sets click listeners of FABs, and 2 bottom sheets
     */
    override fun setActionListeners(applicationKvStore: JsonKvStore?) {
        nearbyParentFragmentView.setFABPlusAction(View.OnClickListener { v: View? ->
            if (applicationKvStore != null && applicationKvStore.getBoolean(
                    "login_skipped", false
                )
            ) {
                // prompt the user to login
                nearbyParentFragmentView.displayLoginSkippedWarning()
            } else {
                nearbyParentFragmentView.animateFABs()
            }
        })

        nearbyParentFragmentView.setFABRecenterAction(View.OnClickListener { v: View? ->
            nearbyParentFragmentView.recenterMap(currentLatLng)
        })
    }

    override fun backButtonClicked(): Boolean {
        if (nearbyParentFragmentView.isAdvancedQueryFragmentVisible()) {
            nearbyParentFragmentView.showHideAdvancedQueryFragment(false)
            return true
        } else if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.listOptionMenuItemClicked()
            return true
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.setBottomSheetDetailsSmaller()
            return true
        }
        return false
    }

    fun markerUnselected() {
        nearbyParentFragmentView.hideBottomSheet()
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
        currentLatLng = if (nearbyParentFragmentView.getMapCenter() != null) {
            nearbyParentFragmentView.getMapCenter()
        } else {
            lastLocation
        }
        if (currentLatLng == null) {
            Timber.d("Skipping update of nearby places as location is unavailable")
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
            var updatedLocationByUser = LocationUtils.deriveUpdatedLocationFromSearchQuery(
                customQuery!!
            )
            if (updatedLocationByUser == null) {
                updatedLocationByUser = lastLocation
            }
            nearbyParentFragmentView.populatePlaces(updatedLocationByUser, customQuery)
        } else if (locationChangeType == LocationChangeType.LOCATION_SIGNIFICANTLY_CHANGED
            || locationChangeType == LocationChangeType.MAP_UPDATED
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

    /**
     * Update places on the map, and asynchronously load their details from cache and Wikidata query
     *
     * @param nearbyPlaces This variable has the list of placecs
     * @param scope the lifecycle scope of `nearbyParentFragment`'s `viewLifecycleOwner`
     */
    fun updateMapMarkers(
        nearbyPlaces: List<Place>?, currentLatLng: LatLng,
        scope: LifecycleCoroutineScope?
    ) {
        val nearbyPlaceGroups = nearbyPlaces?.sortedBy { it.getDistanceInDouble(currentLatLng) }
                ?.take(NearbyController.MAX_RESULTS)
                ?.map {
                    // currently only the place's location is known but bookmarks are stored by name
                    MarkerPlaceGroup(
                        false,
                        it
                    )
                }
                ?: return

        loadPlacesDataAyncJob?.cancel()
        lockUnlockNearby(false) // So that new location updates wont come
        nearbyParentFragmentView.setProgressBarVisibility(false)

        updatePlaceGroupsToControllerAndRender(nearbyPlaceGroups)

        loadPlacesDataAyncJob = scope?.launch(Dispatchers.IO) {
            // clear past clicks and bookmarkChanged queues
            clickedPlaces.clear()
            bookmarkChangedPlaces.clear()
            var clickedPlacesIndex = 0
            var bookmarkChangedPlacesIndex = 0

            val updatedGroups = nearbyPlaceGroups.toMutableList()
            // first load cached places:
            val indicesToUpdate = mutableListOf<Int>()
            for (i in 0..updatedGroups.lastIndex) {
                val repoPlace = placesRepository.fetchPlace(updatedGroups[i].place.entityID)
                if (repoPlace != null && repoPlace.name != ""){
                    updatedGroups[i] = MarkerPlaceGroup(
                        bookmarkLocationDao.findBookmarkLocation(repoPlace),
                        repoPlace
                    )
                } else {
                    indicesToUpdate.add(i)
                }
            }
            if (indicesToUpdate.size < updatedGroups.size) {
                schedulePlacesUpdate(updatedGroups)
            }
            // channel for lists of indices of places, each list to be fetched in a single request
            val fetchPlacesChannel = Channel<List<Int>>(Channel.UNLIMITED)
            var totalBatches = 0
            for (i in indicesToUpdate.indices step LoadPlacesAsyncOptions.BATCH_SIZE) {
                ++totalBatches
                fetchPlacesChannel.send(
                    indicesToUpdate.slice(
                        i until (i + LoadPlacesAsyncOptions.BATCH_SIZE).coerceAtMost(
                            indicesToUpdate.size
                        )
                    )
                )
            }
            fetchPlacesChannel.close()
            val collectResults = Channel<List<Pair<Int, MarkerPlaceGroup>>>(totalBatches)
            repeat(LoadPlacesAsyncOptions.CONNECTION_COUNT) {
                launch(Dispatchers.IO) {
                    for (indices in fetchPlacesChannel) {
                        ensureActive()
                        try {
                            val fetchedPlaces =
                                nearbyController.getPlaces(indices.map { updatedGroups[it].place })
                            collectResults.send(
                                fetchedPlaces.mapIndexed { index, place ->
                                    Pair(indices[index], MarkerPlaceGroup(
                                        bookmarkLocationDao.findBookmarkLocation(place),
                                        place
                                    ))
                                }
                            )
                        } catch (e: Exception) {
                            Timber.tag("NearbyPinDetails").e(e)
                            collectResults.send(indices.map { Pair(it, updatedGroups[it]) })
                        }
                    }
                }
            }
            var collectCount = 0
            for (resultList in collectResults) {
                for ((index, fetchedPlaceGroup) in resultList) {
                    val existingPlace = updatedGroups[index].place
                    val finalPlaceGroup = MarkerPlaceGroup(
                        fetchedPlaceGroup.isBookmarked,
                        fetchedPlaceGroup.place.apply {
                            location = existingPlace.location
                            distance = existingPlace.distance
                            isMonument = existingPlace.isMonument
                        }
                    )
                    updatedGroups[index] = finalPlaceGroup
                    placesRepository
                        .save(finalPlaceGroup.place)
                        .subscribeOn(Schedulers.io())
                        .subscribe()
                }
                // handle any places clicked
                if (clickedPlacesIndex < clickedPlaces.size) {
                    val clickedPlacesBacklog = hashMapOf<LatLng, Place>()
                    while (clickedPlacesIndex < clickedPlaces.size) {
                        clickedPlacesBacklog.put(
                            clickedPlaces[clickedPlacesIndex].location,
                            clickedPlaces[clickedPlacesIndex]
                        )
                        ++clickedPlacesIndex
                    }
                    for ((index, group) in updatedGroups.withIndex()) {
                        if (clickedPlacesBacklog.containsKey(group.place.location)) {
                            updatedGroups[index] = MarkerPlaceGroup(
                                updatedGroups[index].isBookmarked,
                                clickedPlacesBacklog[group.place.location]
                            )
                        }
                    }
                }
                // handle any bookmarks toggled
                if (bookmarkChangedPlacesIndex < bookmarkChangedPlaces.size) {
                    val bookmarkChangedPlacesBacklog = hashMapOf<LatLng, Place>()
                    while (bookmarkChangedPlacesIndex < bookmarkChangedPlaces.size) {
                        bookmarkChangedPlacesBacklog.put(
                            bookmarkChangedPlaces[bookmarkChangedPlacesIndex].location,
                            bookmarkChangedPlaces[bookmarkChangedPlacesIndex]
                        )
                        ++bookmarkChangedPlacesIndex
                    }
                    for ((index, group) in updatedGroups.withIndex()) {
                        if (bookmarkChangedPlacesBacklog.containsKey(group.place.location)) {
                            updatedGroups[index] = MarkerPlaceGroup(
                                bookmarkLocationDao
                                    .findBookmarkLocation(updatedGroups[index].place),
                                updatedGroups[index].place
                            )
                        }
                    }
                }
                schedulePlacesUpdate(updatedGroups)
                if (++collectCount == totalBatches) {
                    break
                }
            }
            collectResults.close()
        }
    }

    /**
     * Some centering task may need to wait for map to be ready, if they are requested before map is
     * ready. So we will remember it when the map is ready
     */
    private fun handleCenteringTaskIfAny() {
        if (!placesLoadedOnce) {
            placesLoadedOnce = true
            nearbyParentFragmentView.centerMapToPlace(null)
        }
    }

    override fun onWikidataEditSuccessful() {
        updateMapAndList(LocationChangeType.MAP_UPDATED)
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

    override fun filterByMarkerType(
        selectedLabels: List<Label?>?, state: Int,
        filterForPlaceState: Boolean, filterForAllNoneType: Boolean
    ) {
        if (filterForAllNoneType) { // Means we will set labels based on states
            when (state) {
                CheckBoxTriStates.UNKNOWN -> {}
                CheckBoxTriStates.UNCHECKED -> {
                    //TODO
                    nearbyParentFragmentView.filterOutAllMarkers()
                    nearbyParentFragmentView.setRecyclerViewAdapterItemsGreyedOut()
                }

                CheckBoxTriStates.CHECKED -> {
                    // Despite showing all labels NearbyFilterState still should be applied
                    nearbyParentFragmentView.filterMarkersByLabels(
                        selectedLabels,
                        filterForPlaceState, false
                    )
                    nearbyParentFragmentView.setRecyclerViewAdapterAllSelected()
                }
            }
        } else {
            nearbyParentFragmentView.filterMarkersByLabels(
                selectedLabels,
                filterForPlaceState, false
            )
        }
    }

    @MainThread
    override fun updateMapMarkersToController(baseMarkers: MutableList<BaseMarker>) {
        NearbyController.markerLabelList.clear()
        for (i in baseMarkers.indices) {
            val nearbyBaseMarker = baseMarkers[i]
            NearbyController.markerLabelList.add(
                MarkerPlaceGroup(
                    bookmarkLocationDao.findBookmarkLocation(nearbyBaseMarker.place),
                    nearbyBaseMarker.place
                )
            )
        }
    }

    /**
     * Sends the supplied markerPlaceGroups to `NearbyController` and nearby list fragment,
     * and tells nearby parent fragment to filter the updated values to be rendered as overlays
     * on the map
     *
     * @param markerPlaceGroups the new/updated list of places along with their bookmarked status
     */
    @MainThread
    private fun updatePlaceGroupsToControllerAndRender(markerPlaceGroups: List<MarkerPlaceGroup>) {
        NearbyController.markerLabelList.clear()
        NearbyController.markerLabelList.addAll(markerPlaceGroups)
        nearbyParentFragmentView.setFilterState()
        nearbyParentFragmentView.updateListFragment(markerPlaceGroups.map { it.place })
    }

    override fun setCheckboxUnknown() {
        nearbyParentFragmentView.setCheckBoxState(CheckBoxTriStates.UNKNOWN)
    }

    override fun setAdvancedQuery(query: String) {
        this.customQuery = query
    }

    override fun searchViewGainedFocus() {
        if (nearbyParentFragmentView.isListBottomSheetExpanded()) {
            // Back should first hide the bottom sheet if it is expanded
            nearbyParentFragmentView.hideBottomSheet()
        } else if (nearbyParentFragmentView.isDetailsBottomSheetVisible()) {
            nearbyParentFragmentView.hideBottomDetailsSheet()
        }
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

    /**
     * Returns true if search this area button is used around our current location, so that we can
     * continue following our current location again
     *
     * @return Returns true if search this area button is used around our current location
     */
    fun searchCloseToCurrentLocation(): Boolean {
        if (null == nearbyParentFragmentView.getLastMapFocus()) {
            return true
        }
        //TODO
        val myLocation = Location("")
        val destLocation = Location("")
        destLocation.latitude = nearbyParentFragmentView.getMapFocus().latitude
        destLocation.longitude = nearbyParentFragmentView.getMapFocus().longitude
        myLocation.latitude = nearbyParentFragmentView.getLastMapFocus().latitude
        myLocation.longitude = nearbyParentFragmentView.getLastMapFocus().longitude
        val distance = myLocation.distanceTo(destLocation)

        return (distance <= 2000.0 * 3 / 4)
    }

    fun onMapReady() {
        initializeMapOperations()
    }

    companion object {
        private val DUMMY = Proxy.newProxyInstance(
            NearbyParentFragmentContract.View::class.java.getClassLoader(),
            arrayOf<Class<*>>(NearbyParentFragmentContract.View::class.java),
            InvocationHandler { proxy: Any?, method: Method?, args: Array<Any?>? ->
                if (method!!.name == "onMyEvent") {
                    return@InvocationHandler null
                } else if (String::class.java == method.returnType) {
                    return@InvocationHandler ""
                } else if (Int::class.java == method.returnType) {
                    return@InvocationHandler 0
                } else if (Int::class.javaPrimitiveType == method.returnType) {
                    return@InvocationHandler 0
                } else if (Boolean::class.java == method.returnType) {
                    return@InvocationHandler java.lang.Boolean.FALSE
                } else if (Boolean::class.javaPrimitiveType == method.returnType) {
                    return@InvocationHandler false
                } else {
                    return@InvocationHandler null
                }
            }
        ) as NearbyParentFragmentContract.View
    }
}
