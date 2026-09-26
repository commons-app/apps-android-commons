package fr.free.nrw.commons.nearby

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel to manage the state of nearby place type filters.
 */
class NearbyFilterViewModel : ViewModel() {

    private val _selectedLabels = MutableLiveData<ArrayList<Label>>(ArrayList())
    val selectedLabels: LiveData<ArrayList<Label>> = _selectedLabels

    /**
     * Updates the selected labels list.
     * @param labels The new list of selected labels
     */
    fun setSelectedLabels(labels: ArrayList<Label>) {
        _selectedLabels.value = labels
    }

    /**
     * Clears all selected labels.
     */
    fun clearSelectedLabels() {
        _selectedLabels.value = ArrayList()
    }

    /**
     * Selects all available labels.
     */
    fun selectAllLabels(allLabels: List<Label>) {
        _selectedLabels.value = ArrayList(allLabels)
    }

    /**
     * Gets the current selected labels as an ArrayList.
     * @return The current selected labels
     */
    fun getSelectedLabels(): ArrayList<Label> {
        return ArrayList(_selectedLabels.value ?: ArrayList())
    }
}
