package fr.free.nrw.commons.nearby.fragments

import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.utils.ViewUtil.showLongToast
import fr.free.nrw.commons.wikidata.WikidataConstants
import timber.log.Timber

/**
 * Applies FAB layout positioning and margins according to bottom sheet state.
 */
internal fun NearbyParentFragment.applyNearbyFabLayout() {
    fabHelper.applyNearbyFabLayout()
}

/**
 * Registers network connectivity broadcast receiver if activity is available.
 */
internal fun NearbyParentFragment.registerNetworkReceiver() {
    if (activity != null) {
        val receiver = broadcastReceiver
        if (receiver != null) {
            requireActivity().registerReceiver(receiver, intentFilter)
        }
    }
}

/**
 * Stores the selected place in shared preferences for camera/upload workflows.
 */
internal fun NearbyParentFragment.storeSharedPrefs(selectedPlace: Place) {
    applicationKvStore.putJson<Place>(WikidataConstants.PLACE_OBJECT, selectedPlace)
    val place = applicationKvStore.getJson<Place>(WikidataConstants.PLACE_OBJECT, Place::class.java)
    Timber.d("Stored place object %s", place.toString())
}

/**
 * Logs an error and displays a long toast with the given message.
 */
internal fun NearbyParentFragment.showErrorMessage(message: String) {
    Timber.e(message)
    showLongToast(requireActivity(), message)
}
