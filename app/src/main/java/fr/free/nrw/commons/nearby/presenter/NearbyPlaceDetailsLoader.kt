package fr.free.nrw.commons.nearby.presenter

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.mwapi.SparqlResponseException
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/**
 * Handles asynchronous loading, caching, batching, and marker synchronization
 * of nearby place details.
 */
class NearbyPlaceDetailsLoader(
    val bookmarkLocationDao: BookmarkLocationsDao,
    val placesRepository: PlacesRepository,
    val nearbyController: NearbyController,
    private val viewProvider: () -> NearbyParentFragmentContract.View,
) {
    val clickedPlaces = CopyOnWriteArrayList<Place>()

    // the currently running job for async loading of pin details, cancelled when new pins are come
    var loadPlacesDataAyncJob: Job? = null

    private var schedulePlacesUpdateJob: Job? = null

    // used to tell the asynchronous place detail loading job that the places' bookmarked status
    // changed so as to prevent inconsistencies
    var bookmarkChangedPlaces = CopyOnWriteArrayList<Place>()

    /**
     * - **batchSize**: number of places to fetch details of in a single request
     * - **connnectionCount**: number of parallel requests. The query service allows 5 per IP, but
     *   on central Paris 3 or 5 made loading several times slower without fewer refusals than 20,
     *   so refusals are handled by the shared pause of [throttle] instead.
     * - **retriesPerLoad**: how many failed batches one load sends again, so that a struggling
     *   service gets only a few more requests
     */
    private object LoadPlacesAsyncOptions {
        const val BATCH_SIZE = 10
        const val CONNECTION_COUNT = 20
        const val RETRIES_PER_LOAD = 3
    }

    /**
     * The pause after a refused or failed request, shared by all loads
     */
    @VisibleForTesting
    internal var throttle = QueryServiceThrottle()

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
        const val SKIP_DELAY_MS = 100L
    }

    /**
     * used to tell the asynchronous place detail loading job that a pin was clicked
     * so as to prevent it from turning grey on the next pin detail update
     *
     * @param place the place whose details have already been loaded because clicked pin
     */
    fun handlePinClicked(place: Place) {
        clickedPlaces.add(place)
    }

    /**
     * Schedules a UI update for the provided list of `MarkerPlaceGroup` objects. Since, the update
     * is performed on the main thread, it waits for a `SchedulePlacesUpdateOptions.skipDelayMs`
     * to see if a new update comes, and if one does, it discards the scheduled UI update.
     *
     * @param markerPlaceGroups The new list of `MarkerPlaceGroup` objects. If the list is empty, no
     *                          update will be performed.
     * @param force Whether to force the update immediately without skipping delay.
     *
     * @see SchedulePlacesUpdateOptions
     */
    suspend fun schedulePlacesUpdate(
        markerPlaceGroups: List<MarkerPlaceGroup>,
        force: Boolean = false,
    ) = withContext(Dispatchers.Main) {
        if (markerPlaceGroups.isEmpty()) return@withContext
        schedulePlacesUpdateJob?.cancel()
        schedulePlacesUpdateJob =
            launch {
                if (!force &&
                    SchedulePlacesUpdateOptions.skippedCount++
                    < SchedulePlacesUpdateOptions.SKIP_LIMIT
                ) {
                    delay(SchedulePlacesUpdateOptions.SKIP_DELAY_MS)
                }
                SchedulePlacesUpdateOptions.skippedCount = 0
                NearbyController.markerLabelList.clear()
                NearbyController.markerLabelList.addAll(markerPlaceGroups)
                viewProvider().setFilterState()
                viewProvider().updateListFragment(markerPlaceGroups.map { it.place })
            }
    }

    /**
     * Iterates through MarkerPlaceGroups and attempts to load the places from the local
     * Place cache/repository. If a Place is in the cache, data from the Place is set into the
     * associated MarkerPlaceGroup. Else, the index is added to the indicesToUpdate list.
     *
     * @param updatedGroups The MarkerPlaceGroups that contain Place entity IDs used to search the
     * local cache for more information about the Place.
     *
     * @param indicesToUpdate The list of indices in updatedGroups where the associated Place
     * was not stored in the local cache and will need to be retrieved in some other way.
     */
    suspend fun loadCachedPlaces(
        updatedGroups: MutableList<MarkerPlaceGroup>,
        indicesToUpdate: MutableList<Int>,
    ) {
        for (i in 0..updatedGroups.lastIndex) {
            val repoPlace = placesRepository.fetchPlace(updatedGroups[i].place.entityID)
            if (repoPlace != null && repoPlace.name != null && repoPlace.name != "") {
                updatedGroups[i].isBookmarked =
                    bookmarkLocationDao.findBookmarkLocation(repoPlace.name)

                updatedGroups[i].place.apply {
                    name = repoPlace.name
                    label = repoPlace.label
                    isMonument = repoPlace.isMonument
                    pic = repoPlace.pic ?: ""
                    exists = repoPlace.exists ?: true
                    longDescription = repoPlace.longDescription ?: ""
                    language = repoPlace.language
                    siteLinks = repoPlace.siteLinks
                }
            } else {
                indicesToUpdate.add(i)
            }
        }
    }

    /**
     * Creates a Channel object that contains numbers divided into separate batches/lists.
     * Each batch/list will have a maximum size.
     *
     * @param numbers The list of numbers that will be divided into multiple batches/lists.
     * @param batchSize The maximum size of each batch/list which the numbers will be placed into.
     *
     * @return The Channel object which contains batches/lists of numbers.
     */
    suspend fun createBatches(
        numbers: MutableList<Int>,
        batchSize: Int,
    ): Channel<List<Int>> {
        val batches = Channel<List<Int>>(Channel.UNLIMITED)

        for (i in numbers.indices step batchSize) {
            batches.send(
                numbers.slice(
                    i until
                        (i + batchSize).coerceAtMost(
                            numbers.size,
                        ),
                ),
            )
        }

        batches.close()
        return batches
    }

    /**
     * Method used by an individual thread/coroutine to request batch Place data from WikiData.
     * This is intended to be used by many threads in parallel to speed up data retrieval.
     *
     * A batch that fails for a temporary reason is sent again once after the pause of [throttle],
     * while [retries] are left. Other failures are not retried. The places of a batch that isn't
     * loaded keep their current data. Batches are not split into single places, as after a refusal
     * that would only send more requests.
     *
     * @param batchedIndicesToFetch Contains the batches of indices. These indices correspond to
     * Places in updatedGroups that need more information fetched from WikiData.
     * @param updatedGroups List containing incomplete Place data
     * @param collectResults Holds the Place data fetched from Wikidata.
     * @param retries How many failed batches this load can still send again
     */
    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchPlacesThreadMethod(
        batchedIndicesToFetch: Channel<List<Int>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
        collectResults: Channel<List<Pair<Int, Place?>>>,
        retries: AtomicInteger,
    ) = coroutineScope {
        for (indices in batchedIndicesToFetch) {
            ensureActive()
            val fetchedPlaces =
                try {
                    nearbyController.getPlacesThrottled(indices.map { updatedGroups[it].place }, throttle, retries)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    Timber.tag("NearbyPinDetails").e(e)
                    null
                }
            // The query gives one row per class, picture etc. in no fixed order, so match by QID
            val fetchedPlacesById = fetchedPlaces.orEmpty().groupBy { it.wikiDataEntityId }
            val usedIds = mutableSetOf<String?>()
            collectResults.send(
                indices.map { i ->
                    Pair(i, fetchedPlacesById.placeFor(updatedGroups[i].place.wikiDataEntityId, usedIds))
                },
            )
        }
    }

    /**
     * Launches a specific number of threads to retrieve Place data in parallel from the
     * WikiData servers. It does not wait for them, so that results can be processed as they arrive.
     *
     * @param numThreads The number of threads to launch in parallel
     * @param batchedIndicesToFetch Contains the batches of indices. These indices correspond to
     * Places in updatedGroups that need more information fetched from WikiData.
     * @param updatedGroups List containing Place data
     * @param collectResults Holds the Place data fetched from Wikidata.
     */
    private fun CoroutineScope.launchFetchPlacesThreads(
        numThreads: Int,
        batchedIndicesToFetch: Channel<List<Int>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
        collectResults: Channel<List<Pair<Int, Place?>>>,
    ) {
        val retries = AtomicInteger(LoadPlacesAsyncOptions.RETRIES_PER_LOAD)
        repeat(numThreads) {
            launch(Dispatchers.IO) {
                fetchPlacesThreadMethod(batchedIndicesToFetch, updatedGroups, collectResults, retries)
            }
        }
    }

    /**
     * Places the fetched WikiData Place data into associated MarkerPlaceGroups
     *
     * @param resultList The Place data fetched from WikiData, null for places that could not be
     * loaded: these keep their current data and are not saved to the cache
     * @param updatedGroups The MarkerPlaceGroups which will be updated with the data fetched
     * from WikiData
     */
    private suspend fun processResults(
        resultList: List<Pair<Int, Place?>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
    ) {
        for ((index, fetchedPlace) in resultList) {
            fetchedPlace ?: continue
            val existingPlace = updatedGroups[index].place
            val finalPlaceGroup =
                MarkerPlaceGroup(
                    // Read it now, not when the batch was fetched: the user may have changed it since
                    bookmarkLocationDao.findBookmarkLocation(fetchedPlace.name),
                    fetchedPlace.apply {
                        location = existingPlace.location
                        distance = existingPlace.distance
                        isMonument = existingPlace.isMonument
                    },
                )
            updatedGroups[index] = finalPlaceGroup
            placesRepository
                .save(finalPlaceGroup.place)
                .subscribeOn(Schedulers.io())
                .subscribe()
        }
    }

    /**
     * Ensures the correct bookmark boolean value for Places whose bookmarks have changed.
     * This bookmark information is retrieved from the bookmarkLocationDao.
     *
     * @param updatedGroups The MarkerPlaceGroups which will be displayed on the map to the user.
     * @param bookmarkChangedPlacesIndex Only Places at indices greater than or equal to this number
     * in bookmarkChangedPlaces will be handled. This can be useful for avoiding redundant
     * computation when calling this method multiple times.
     *
     * @return The updated bookmarkChangedPlacesIndex used in future calls to this method.
     */
    private suspend fun handleBookmarksToggled(
        updatedGroups: MutableList<MarkerPlaceGroup>,
        bookmarkChangedPlacesIndex: Int,
    ): Int {
        var i = bookmarkChangedPlacesIndex
        if (i < bookmarkChangedPlaces.size) {
            val bookmarkChangedPlacesBacklog = bookmarkChangedPlaces.toLocationMap(i)

            i += bookmarkChangedPlacesBacklog.size
            for ((index, group) in updatedGroups.withIndex()) {
                if (bookmarkChangedPlacesBacklog.containsKey(group.place.location)) {
                    updatedGroups[index] =
                        MarkerPlaceGroup(
                            bookmarkLocationDao
                                .findBookmarkLocation(updatedGroups[index].place.name),
                            updatedGroups[index].place,
                        )
                }
            }
        }

        return i
    }

    /**
     * Ensures any clicked Places are updated using existing Place data
     * and not by data from regular/batched WikiData server responses.
     *
     * @param updatedGroups The MarkerPlaceGroups which will be displayed on the map to the user.
     * @param clickedPlacesIndex Only Places at indices greater than or equal to this number
     * in clickedPlaces will be handled. This can be useful for avoiding redundant computation when
     * calling this method multiple times.
     *
     * @return The updated clickedPlacesIndex used in future calls to this method.
     */
    private fun handlePlacesClicked(
        updatedGroups: MutableList<MarkerPlaceGroup>,
        clickedPlacesIndex: Int,
    ): Int {
        var i = clickedPlacesIndex
        if (i < clickedPlaces.size) {
            val clickedPlacesBacklog = clickedPlaces.toLocationMap(i)

            i += clickedPlacesBacklog.size
            for ((index, group) in updatedGroups.withIndex()) {
                if (clickedPlacesBacklog.containsKey(group.place.location)) {
                    updatedGroups[index] =
                        MarkerPlaceGroup(
                            updatedGroups[index].isBookmarked,
                            clickedPlacesBacklog[group.place.location],
                        )
                }
            }
        }

        return i
    }

    /**
     * Load the places' details from cache and Wikidata query, and update these details on the map
     * as and when they arrive.
     *
     * @param nearbyPlaceGroups The list of `MarkerPlaceGroup` objects to be rendered on the map.
     * Note that the supplied objects' `isBookmarked` property can be set false as the actual
     * value is retrieved from the bookmarks db eventually.
     * @param scope the lifecycle scope of `nearbyParentFragment`'s `viewLifecycleOwner`
     *
     * @see LoadPlacesAsyncOptions
     */
    fun loadPlacesDataAsync(
        nearbyPlaceGroups: List<MarkerPlaceGroup>,
        scope: LifecycleCoroutineScope?,
    ) {
        loadPlacesDataAyncJob?.cancel()
        loadPlacesDataAyncJob =
            scope?.launch(Dispatchers.IO) {
                // clear past clicks and bookmarkChanged queues
                clickedPlaces.clear()
                bookmarkChangedPlaces.clear()

                val updatedGroups = nearbyPlaceGroups.toMutableList()
                // first load cached places:
                val indicesToUpdate = mutableListOf<Int>()

                loadCachedPlaces(updatedGroups, indicesToUpdate)

                schedulePlacesUpdate(updatedGroups, force = true)
                // channel for lists of indices of places, each list to be fetched in a single request
                val batchedIndicesToFetch =
                    createBatches(indicesToUpdate, LoadPlacesAsyncOptions.BATCH_SIZE)

                val collectResults = Channel<List<Pair<Int, Place?>>>(Channel.Factory.UNLIMITED)

                launchFetchPlacesThreads(
                    LoadPlacesAsyncOptions.CONNECTION_COUNT,
                    batchedIndicesToFetch,
                    updatedGroups,
                    collectResults,
                )

                var collectCount = 0
                var clickedPlacesIndex = 0
                var bookmarkChangedPlacesIndex = 0
                while (collectCount < indicesToUpdate.size) {
                    val resultList = collectResults.receive()

                    processResults(resultList, updatedGroups)

                    clickedPlacesIndex = handlePlacesClicked(updatedGroups, clickedPlacesIndex)

                    bookmarkChangedPlacesIndex =
                        handleBookmarksToggled(
                            updatedGroups,
                            bookmarkChangedPlacesIndex,
                        )
                    schedulePlacesUpdate(updatedGroups)
                    collectCount += resultList.size
                }
                collectResults.close()
            }
    }
}

/**
 * Creates a HashMap mapping place locations (LatLng) to the associated places.
 *
 * @param startIndex Only Places with indices between startIndex and places.lastIndex
 * (inclusive) will be used to create the HashMap. This can be useful for avoiding redundant
 * computation when calling this method on the same places list multiple times.
 *
 * @return The HashMap containing (Place's LatLng, Place) entries.
 */
private fun List<Place>.toLocationMap(startIndex: Int): HashMap<LatLng, Place> {
    val map = hashMapOf<LatLng, Place>()
    for (i in startIndex..lastIndex) {
        map[this[i].location] = this[i]
    }
    return map
}

/**
 * Returns the fetched Place for the item [id], or a copy of it if an earlier pin already got it:
 * a custom query can put the same item on two pins, and each pin needs its own Place.
 *
 * @param usedIds The ids already given to a pin of the same batch.
 */
private fun Map<String?, List<Place>>.placeFor(
    id: String?,
    usedIds: MutableSet<String?>,
): Place? = this[id]?.first()?.let { if (usedIds.add(id)) it else it.copy() }

/**
 * Returns a new Place with the details of this one, so that two pins never share one object.
 */
private fun Place.copy(): Place =
    Place(language, name, label, longDescription, location, category, siteLinks, pic, exists, entityID)

private const val TOO_MANY_REQUESTS = 429
private const val MAX_WAIT_DURING_LOAD_MINUTES = 2L

/**
 * Pauses up to this are waited out in the background while the map stays usable. A longer one means
 * the service is down: the load ends and the places not loaded yet keep their data until the next.
 */
private val MAX_WAIT_DURING_LOAD_MILLIS = TimeUnit.MINUTES.toMillis(MAX_WAIT_DURING_LOAD_MINUTES)

/**
 * Fetches the places once the pause of [throttle] is over. A temporary failure pauses all requests,
 * and the request is sent once more if [retries] are left and the pause isn't too long.
 */
private suspend fun NearbyController.getPlacesThrottled(
    places: List<Place>,
    throttle: QueryServiceThrottle,
    retries: AtomicInteger,
): List<Place> {
    var retried = false
    while (true) {
        val wait = throttle.waitMillis()
        val error =
            if (wait > MAX_WAIT_DURING_LOAD_MILLIS) {
                IOException("Query service paused for $wait ms")
            } else {
                delay(wait)
                try {
                    return getPlaces(places).also { throttle.onSuccess() }
                } catch (e: IOException) {
                    e
                }
            }
        if (wait <= MAX_WAIT_DURING_LOAD_MILLIS && error.isTemporary()) {
            val pause = throttle.onTemporaryFailure((error as? SparqlResponseException)?.retryAfterMillis)
            if (!retried && pause <= MAX_WAIT_DURING_LOAD_MILLIS && retries.getAndDecrement() > 0) {
                retried = true
                continue
            }
        }
        throw error
    }
}

/**
 * Refusals for a while (429, 502, 503, 504) and failures on the way (timeouts, lost connections)
 * can work a bit later. Other answers, like a query the service rejects, would fail again.
 */
private fun IOException.isTemporary() =
    when (this) {
        is SparqlResponseException ->
            code == TOO_MANY_REQUESTS ||
                code in HttpURLConnection.HTTP_BAD_GATEWAY..HttpURLConnection.HTTP_GATEWAY_TIMEOUT
        else -> true
    }
