package fr.free.nrw.commons.nearby.presenter

import fr.free.nrw.commons.nearby.CheckBoxTriStates
import fr.free.nrw.commons.nearby.Label
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract

/**
 * Handles marker and label filtering logic for NearbyParentFragment.
 */
class NearbyFilterHandler(
    private val viewProvider: () -> NearbyParentFragmentContract.View,
) : NearbyFilterOperations {
    override fun filterByMarkerType(
        selectedLabels: List<Label?>?,
        state: Int,
        filterForPlaceState: Boolean,
        filterForAllNoneType: Boolean,
    ) {
        val view = viewProvider()
        if (filterForAllNoneType) { // Means we will set labels based on states
            when (state) {
                CheckBoxTriStates.UNKNOWN -> {}
                CheckBoxTriStates.UNCHECKED -> {
                    view.filterOutAllMarkers()
                    view.setRecyclerViewAdapterItemsGreyedOut()
                }

                CheckBoxTriStates.CHECKED -> {
                    // Despite showing all labels NearbyFilterState still should be applied
                    view.filterMarkersByLabels(
                        selectedLabels,
                        filterForPlaceState,
                        false,
                    )
                    view.setRecyclerViewAdapterAllSelected()
                }
            }
        } else {
            view.filterMarkersByLabels(
                selectedLabels,
                filterForPlaceState,
                false,
            )
        }
    }

    override fun setCheckboxUnknown() {
        viewProvider().setCheckBoxState(CheckBoxTriStates.UNKNOWN)
    }
}
