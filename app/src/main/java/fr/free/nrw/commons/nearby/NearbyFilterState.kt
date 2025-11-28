package fr.free.nrw.commons.nearby

object NearbyFilterState {
    var isExistsSelected: Boolean = true
        private set
    var isNeedPhotoSelected: Boolean = true
        private set
    var isWlmSelected: Boolean = true
        private set
    val checkBoxTriState: Int = -1
    var selectedLabels: List<Label> = emptyList()
        private set

    fun setSelectedLabels(labels: List<Label>) {
        selectedLabels = labels
    }

    fun setExistsSelected(existsSelected: Boolean) {
        isExistsSelected = existsSelected
    }

    fun setNeedPhotoSelected(needPhotoSelected: Boolean) {
        isNeedPhotoSelected = needPhotoSelected
    }

    fun setWlmSelected(wlmSelected: Boolean) {
        isWlmSelected = wlmSelected
    }
}
