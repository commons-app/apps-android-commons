package fr.free.nrw.commons.customselector.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.customselector.data.MediaReader
import fr.free.nrw.commons.customselector.model.Image
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CustomSelectorViewModel(private val mediaReader: MediaReader): ViewModel() {

    private val _uiState = MutableStateFlow(CustomSelectorState())
    val uiState = _uiState.asStateFlow()

    private val foldersMap = mutableMapOf<Long, MutableList<Image>>()

    init {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            mediaReader.getImages().collect { image->
                val bucketId = image.bucketId
                foldersMap.getOrPut(bucketId) { mutableListOf() }.add(image)
            }
            val foldersList = foldersMap.map { (bucketId, images)->
                val firstImage = images.first()
                Folder(
                    bucketId = bucketId, bucketName = firstImage.bucketName,
                    preview = firstImage.uri, itemsCount = images.size
                )
            }
            _uiState.update { it.copy(isLoading = false, folders = foldersList) }
        }
    }

    fun onEvent(e: CustomSelectorEvent) {
        when(e) {
            is CustomSelectorEvent.OnFolderClick-> {
                _uiState.update {
                    it.copy(filteredImages = foldersMap[e.bucketId]?.toList() ?: emptyList())
                }
            }

            is CustomSelectorEvent.OnImageSelection -> {
                _uiState.update { state ->
                    val updatedSelectedIds = if (state.selectedImageIds.contains(e.imageId)) {
                        state.selectedImageIds - e.imageId // Remove if already selected
                    } else{
                        state.selectedImageIds + e.imageId // Add if not selected
                    }
                    state.copy(selectedImageIds = updatedSelectedIds)
                }
            }

            is CustomSelectorEvent.OnDragImageSelection-> {
                _uiState.update { it.copy(selectedImageIds = e.imageIds) }
            }

            else -> {}
        }
    }
}