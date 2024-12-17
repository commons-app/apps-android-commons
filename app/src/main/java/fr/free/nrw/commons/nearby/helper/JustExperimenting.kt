package fr.free.nrw.commons.nearby.helper

import androidx.lifecycle.lifecycleScope
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
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

class JustExperimenting(frag: NearbyParentFragment) {
    private val scope = frag.lifecycleScope

    private var skippedCount = 0;
    private val skipLimit = 2;
    private val skipDelayMs = 1000L;

    private var markersState = MutableStateFlow(emptyList<Marker>());
    private val markerBaseDataChannel = Channel<ArrayList<MarkerPlaceGroup>>(Channel.CONFLATED);


    fun loadNewMarkers(es: ArrayList<MarkerPlaceGroup>) = scope.launch {
        markerBaseDataChannel.send(es)
    }
    fun updateMarkersState(markers: List<Marker>){
        markersState.value = markers
    }
    init {
        scope.launch {
            markersState.collectLatest {
                if(skippedCount++<skipLimit){
                    delay(skipDelayMs);
                }
                skippedCount = 0;
                Timber.tag("temptagtwo").d("here: ${it.size}")
                frag.replaceMarkerOverlays(it);
            }
        }
        scope.launch {
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
                    markerBaseDataList.sortBy {
                        it.place.getDistanceInDouble(frag.mapFocus)
                    }

                    val batchSize = 3

                    for (i in markerBaseDataList.indices step batchSize) {
                        ensureActive()
                        // TODO
                    }
                }
            }
        }
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