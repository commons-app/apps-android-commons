package fr.free.nrw.commons.nearby.presenter

import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlacesRepository
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList

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
     * - **connnectionCount**: number of parallel requests
     */
    private object LoadPlacesAsyncOptions {
        const val BATCH_SIZE = 10
        const val CONNECTION_COUNT = 20
    }

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
     * If a WikiData server error occurs, this method will launch individual threads for each Place
     * because a single server error for one Place can cause WikiData to respond with an error
     * for the entire batch.
     *
     * @param batchedIndicesToFetch Contains the batches of indices. These indices correspond to
     * Places in updatedGroups that need more information fetched from WikiData.
     * @param updatedGroups List containing incomplete Place data
     * @param collectResults Holds the Place data fetched from Wikidata.
     */
    private suspend fun fetchPlacesThreadMethod(
        batchedIndicesToFetch: Channel<List<Int>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
        collectResults: Channel<List<Pair<Int, MarkerPlaceGroup>>>,
    ) = coroutineScope {
        for (indices in batchedIndicesToFetch) {
            ensureActive()
            val fetchedPlacesResult =
                runCatching {
                    nearbyController.getPlaces(indices.map { updatedGroups[it].place })
                }
            if (fetchedPlacesResult.isSuccess) {
                val fetchedPlaces = fetchedPlacesResult.getOrThrow()
                collectResults.send(
                    fetchedPlaces.mapIndexed { index, place ->
                        Pair(
                            indices[index],
                            MarkerPlaceGroup(
                                bookmarkLocationDao.findBookmarkLocation(place.name),
                                place,
                            ),
                        )
                    },
                )
            } else {
                Timber.tag("NearbyPinDetails").e(fetchedPlacesResult.exceptionOrNull())
                // HTTP request failed. Try individual places
                for (i in indices) {
                    launch {
                        val onePlaceBatch = mutableListOf<Pair<Int, MarkerPlaceGroup>>()
                        val onePlaceResult =
                            runCatching {
                                nearbyController.getPlaces(
                                    mutableListOf(updatedGroups[i].place),
                                )
                            }
                        if (onePlaceResult.isSuccess) {
                            val fetchedPlace = onePlaceResult.getOrThrow()
                            onePlaceBatch.add(
                                Pair(
                                    i,
                                    MarkerPlaceGroup(
                                        bookmarkLocationDao.findBookmarkLocation(
                                            fetchedPlace[0].name,
                                        ),
                                        fetchedPlace[0],
                                    ),
                                ),
                            )
                        } else {
                            Timber.tag("NearbyPinDetails").e(onePlaceResult.exceptionOrNull())
                            onePlaceBatch.add(Pair(i, updatedGroups[i]))
                        }
                        collectResults.send(onePlaceBatch)
                    }
                }
            }
        }
    }

    /**
     * Launches a specific number of threads to retrieve Place data in parallel from the
     * WikiData servers.
     *
     * @param numThreads The number of threads to launch in parallel
     * @param batchedIndicesToFetch Contains the batches of indices. These indices correspond to
     * Places in updatedGroups that need more information fetched from WikiData.
     * @param updatedGroups List containing Place data
     * @param collectResults Holds the Place data fetched from Wikidata.
     */
    private suspend fun launchFetchPlacesThreads(
        numThreads: Int,
        batchedIndicesToFetch: Channel<List<Int>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
        collectResults: Channel<List<Pair<Int, MarkerPlaceGroup>>>,
    ) = coroutineScope {
        repeat(numThreads) {
            launch(Dispatchers.IO) {
                fetchPlacesThreadMethod(batchedIndicesToFetch, updatedGroups, collectResults)
            }
        }
    }

    /**
     * Places the fetched WikiData Place data into associated MarkerPlaceGroups
     *
     * @param resultList The Place data fetched from WikiData
     * @param updatedGroups The MarkerPlaceGroups which will be updated with the data fetched
     * from WikiData
     */
    private fun processResults(
        resultList: List<Pair<Int, MarkerPlaceGroup>>,
        updatedGroups: MutableList<MarkerPlaceGroup>,
    ) {
        for ((index, fetchedPlaceGroup) in resultList) {
            val existingPlace = updatedGroups[index].place
            val finalPlaceGroup =
                MarkerPlaceGroup(
                    fetchedPlaceGroup.isBookmarked,
                    fetchedPlaceGroup.place.apply {
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

                val collectResults = Channel<List<Pair<Int, MarkerPlaceGroup>>>(Channel.Factory.UNLIMITED)

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
 * @param startIndex Only Places with indices between startIndex and lastIndex
 * will be used to create the HashMap.
 * @return The HashMap containing (Place's LatLng, Place) entries.
 */
private fun List<Place>.toLocationMap(startIndex: Int): HashMap<LatLng, Place> {
    val map = hashMapOf<LatLng, Place>()
    for (i in startIndex..lastIndex) {
        map[this[i].location] = this[i]
    }
    return map
}
