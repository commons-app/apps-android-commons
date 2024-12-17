package fr.free.nrw.commons.nearby.helper

import androidx.lifecycle.lifecycleScope
import fr.free.nrw.commons.databinding.FragmentNearbyParentBinding
import fr.free.nrw.commons.nearby.fragments.NearbyParentFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.osmdroid.views.overlay.Marker
import timber.log.Timber

class JustExperimenting(frag: NearbyParentFragment) {
    private var markersState = MutableStateFlow(emptyList<Marker>());
    private var skippedCount = 0;
    fun updateMarkersState(markers: List<Marker>){
        markersState.value = markers
    }
    init {
        frag.lifecycleScope.launch(Dispatchers.Default) {
            markersState.collectLatest {
                ++skippedCount;
                if(skippedCount<5){
                    delay(500);
                }
                skippedCount = 0;
                Timber.tag("temptagtwo").d("here: ${it.size}")
                frag.experimentingPartTwo(it);
            }
        }
    }
}