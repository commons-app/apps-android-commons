package fr.free.nrw.commons.nearby.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.osmdroid.views.overlay.Marker
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArrayList

class JustExperimenting(frag: NearbyParentFragment) {
    private val scope = frag.viewLifecycleOwner.lifecycleScope

    private var skippedCount = 0
    private val skipLimit = 2
    private val skipDelayMs = 1000L

    private var markersState = MutableStateFlow(emptyList<Marker>())
    private val markerBaseDataChannel = Channel<ArrayList<MarkerPlaceGroup>>(Channel.CONFLATED)

    private val clickedPlaces = CopyOnWriteArrayList<Place>()
    fun handlePlaceClicked(place: Place) {
        clickedPlaces.add(place)
    }

    fun loadNewMarkers(es: ArrayList<MarkerPlaceGroup>) = scope.launch {
        markerBaseDataChannel.send(es)
    }
    fun updateMarkersState(markers: List<Marker>){
        Timber.tag("nearbyperformancefixes").d("should be here in a bit")
        markersState.value = markers
    }
    init {
        scope.launch(Dispatchers.Default) {
            markersState.collectLatest {
                Timber.tag("nearbyperformancefixes").d("here lol")
                if (it.isEmpty()) {
                    return@collectLatest
                }
//                if (skippedCount++ < skipLimit) {
//                    delay(skipDelayMs)
//                }
                skippedCount = 0
                Timber.tag("temptagtwo").d("here: ${it.size}")
                frag.replaceMarkerOverlays(it)
            }
        }
        scope.launch(Dispatchers.Default) {
            var loadPinDetailsJob: Job? = null
            for(markerBaseDataList in markerBaseDataChannel) {
                loadPinDetailsJob?.cancel()
                loadPinDetailsJob = launch {
                    loadPinsDetails(frag, markerBaseDataList, this)
                }
            }
        }

        frag.viewLifecycleOwner.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                performCleanup()
            }
        })
    }

    private suspend fun loadPinsDetails(
        frag: NearbyParentFragment,
        markerBaseDataList: ArrayList<MarkerPlaceGroup>,
        scope: CoroutineScope
    ) {
        // make sure the grey pins are loaded immediately:
        skippedCount = skipLimit
        updateMarkersState(
            markerBaseDataList.map {
                frag.convertToMarker(it.place, it.isBookmarked)
            }
        )

        // now load the pin details:
        clickedPlaces.clear()
        var clickedPlacesIndex = 0
        markerBaseDataList.sortBy {
            it.place.getDistanceInDouble(frag.mapFocus)
        }
        val updatedMarkers = ArrayList<Marker>(markerBaseDataList.size)
        markerBaseDataList.forEach {
            updatedMarkers.add(frag.convertToMarker(it.place, it.isBookmarked))
        }

        val batchSize = 3
        var currentIndex = 0
        val endIndex = markerBaseDataList.lastIndex
        Timber.tag("nearbyperformancefixes").d("loaded %d gray pins", endIndex+1)
        while (currentIndex <= endIndex) {
            Timber.tag("nearbyperformancefixes").d("loading pins from %d", currentIndex)
            scope.ensureActive()
            val toUpdateMarkersFrom = currentIndex

            val placesToFetch = mutableListOf<Int>()
            while (currentIndex<=endIndex && placesToFetch.size < batchSize) {
                val existingPlace = markerBaseDataList[currentIndex].place
                if (existingPlace.name != "") {
                    ++currentIndex
                    continue
                }
                val repoPlace = withContext(Dispatchers.IO) {
                    frag.getPlaceFromRepository(existingPlace.entityID)
                }
                if (repoPlace != null && repoPlace.name != ""){
                    markerBaseDataList[currentIndex] =
                        MarkerPlaceGroup(markerBaseDataList[currentIndex].isBookmarked, repoPlace)
                    ++currentIndex
                    continue
                }
                placesToFetch.add(currentIndex)
                ++currentIndex
            }
            if (placesToFetch.isNotEmpty()) {
                val fetchedPlaces = withContext(Dispatchers.IO) {
                    frag.getPlacesFromController(placesToFetch.map {
                        markerBaseDataList[it].place
                    })
                }
                scope.ensureActive()
                for (fetchedPlace in fetchedPlaces) {
                    for (index in placesToFetch) { // nesting okay here as batch size is small
                        val existingPlace = markerBaseDataList[index].place
                        if (existingPlace.siteLinks.wikidataLink == fetchedPlace.siteLinks.wikidataLink){
                            fetchedPlace.location = existingPlace.location
                            fetchedPlace.distance = existingPlace.distance
                            fetchedPlace.isMonument = existingPlace.isMonument
                            markerBaseDataList[index] = MarkerPlaceGroup(markerBaseDataList[index].isBookmarked, fetchedPlace)
                            frag.savePlaceToDatabase(fetchedPlace)
                        }
                    }
                }
            }
            for (i in toUpdateMarkersFrom..<currentIndex) {
                updatedMarkers[i] = frag.convertToMarker(markerBaseDataList[i].place, markerBaseDataList[i].isBookmarked)
            }
            if(clickedPlacesIndex < clickedPlaces.size) {
                val clickedPlacesBacklog = hashMapOf<LatLng, Place>()
                while (clickedPlacesIndex < clickedPlaces.size) {
                    clickedPlacesBacklog.put(clickedPlaces[clickedPlacesIndex].location, clickedPlaces[clickedPlacesIndex])
                }
                for (i in currentIndex..endIndex) {
                    if (clickedPlacesBacklog.containsKey(markerBaseDataList[i].place.location)) {
                        markerBaseDataList[i] = MarkerPlaceGroup(markerBaseDataList[i].isBookmarked, clickedPlacesBacklog[markerBaseDataList[i].place.location])
                        updatedMarkers[i] = frag.convertToMarker(markerBaseDataList[i].place, markerBaseDataList[i].isBookmarked)
                    }
                }
            }
            updateMarkersState(updatedMarkers)
        }
    }

    private fun performCleanup() {
        markerBaseDataChannel.close()
    }

//    private val mapEventsOverlay = frag.mapEventsOverlay
//    fun getBaseOverlays(view: MapView): List<Overlay> = listOf(
//        // distance scale
//        ScaleBarOverlay(view).apply {
//            setScaleBarOffset(15, 25)
//            setBackgroundPaint(Paint().apply { setARGB(200, 255, 250, 250) })
//            enableScaleBar()
//        },
//        // map events overlay:
//        mapEventsOverlay
//    )

}