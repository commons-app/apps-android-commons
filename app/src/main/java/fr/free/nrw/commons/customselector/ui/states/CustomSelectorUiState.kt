package fr.free.nrw.commons.customselector.ui.states

import fr.free.nrw.commons.customselector.ui.screens.Folder
import fr.free.nrw.commons.customselector.ui.screens.imageId

typealias isNotForUpload = Boolean

data class CustomSelectorUiState(
    val isLoading: Boolean = true,
    val folders: List<Folder> = emptyList(),
    val filteredImages: List<ImageUiState> = emptyList(),
    val selectedImageIds: Set<Long> = emptySet(),
    val imagesNotForUpload: Map<imageId, isNotForUpload> = emptyMap(),
    val shouldShowHandledPictures: Boolean = true
) {
    val inSelectionMode: Boolean
        get() = selectedImageIds.isNotEmpty()
}