package fr.free.nrw.commons.nearby.helper

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Marker
import timber.log.Timber
import java.util.ArrayList
import java.util.concurrent.CopyOnWriteArraySet

class JustExperimenting(frag: NearbyParentFragment) {
    private val scope = frag.viewLifecycleOwner.lifecycleScope

    private var skippedCount = 0
    private val skipLimit = 2
    private val skipDelayMs = 1000L

    private var markersState = MutableStateFlow(emptyList<Marker>())
    private val markerBaseDataChannel = Channel<ArrayList<MarkerPlaceGroup>>(Channel.CONFLATED)

    private val clickedPlaces = CopyOnWriteArraySet<Place>()
    fun handlePlaceClicked(place: Place) {
        clickedPlaces.add(place)
    }

    fun loadNewMarkers(es: ArrayList<MarkerPlaceGroup>) = scope.launch {
        markerBaseDataChannel.send(es)
    }
    fun updateMarkersState(markers: List<Marker>){
        markersState.value = markers
    }
    init {
        scope.launch(Dispatchers.Default) {
            markersState.collectLatest {
                if (it.isEmpty()) {
                    return@collectLatest
                }
                if (skippedCount++ < skipLimit) {
                    delay(skipDelayMs)
                }
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
                    while (currentIndex <= endIndex) {
                        ensureActive()

                        val placesToProcess = HashMap<Int, Place>()
//                        while(currentIndex<=endIndex && )
                        ++currentIndex // remove this, added just for testing
                    }
                }
            }
        }

        frag.viewLifecycleOwner.lifecycle.addObserver(object: DefaultLifecycleObserver {
            override fun onDestroy(owner: LifecycleOwner) {
                performCleanup()
            }
        })
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