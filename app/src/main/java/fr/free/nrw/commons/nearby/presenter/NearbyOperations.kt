package fr.free.nrw.commons.nearby.presenter

import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.kvstore.JsonKvStore
import fr.free.nrw.commons.location.LatLng
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.MarkerPlaceGroup
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract
import fr.free.nrw.commons.wikidata.WikidataEditListener.WikidataP18EditListener

/**
 * Contract for UI and bottom-sheet interactions in NearbyParentFragment.
 */
interface NearbyUiOperations {
    fun attachView(view: NearbyParentFragmentContract.View)

    fun detachView()

    fun setActionListeners(applicationKvStore: JsonKvStore?)

    fun removeNearbyPreferences(applicationKvStore: JsonKvStore)

    fun backButtonClicked(): Boolean

    fun searchViewGainedFocus()

    fun markerUnselected()
}

/**
 * Contract for marker and label filtering in NearbyParentFragment.
 */
interface NearbyFilterOperations {
    fun filterByMarkerType(
        selectedLabels: List<Label?>?,
        state: Int,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean,
    )

    fun setCheckboxUnknown()
}

/**
 * Contract for place presentation, bookmark operations, and cache cleanup in NearbyParentFragment.
 */
interface NearbyPlaceOperations : WikidataP18EditListener {
    fun toggleBookmarkedStatus(
        place: Place?,
        scope: LifecycleCoroutineScope?,
    )

    fun removeOldCachedPlaces(applicationKvStore: JsonKvStore)

    fun handlePinClicked(place: Place)

    fun loadPlacesDataAsync(
        places: List<MarkerPlaceGroup>,
        scope: LifecycleCoroutineScope?,
    )

    fun updateMapMarkers(
        nearbyPlaces: List<Place>?,
        currentLatLng: LatLng,
        scope: LifecycleCoroutineScope?,
    )

    fun searchCloseToCurrentLocation(): Boolean
}
