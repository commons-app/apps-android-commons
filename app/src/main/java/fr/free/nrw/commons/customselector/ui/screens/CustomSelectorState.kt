package fr.free.nrw.commons.customselector.ui.screens

import fr.free.nrw.commons.customselector.model.Image

data class CustomSelectorState(
    val isLoading: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val filteredImages: List<Image> = emptyList()
)