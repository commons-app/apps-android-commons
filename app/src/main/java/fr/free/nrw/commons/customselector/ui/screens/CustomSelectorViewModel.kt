package fr.free.nrw.commons.customselector.ui.screens

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.free.nrw.commons.customselector.domain.ImageRepository
import fr.free.nrw.commons.customselector.domain.model.Image
import fr.free.nrw.commons.customselector.domain.use_case.ImageUseCase
import fr.free.nrw.commons.customselector.ui.states.CustomSelectorUiState
import fr.free.nrw.commons.customselector.ui.states.ImageUiState
import fr.free.nrw.commons.customselector.ui.states.toImageUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias imageId = Long
typealias imageSHA = String

class CustomSelectorViewModel @Inject constructor(
    private val imageRepository: ImageRepository,
    private val imageUseCase: ImageUseCase
): ViewModel() {

    private val _uiState = MutableStateFlow(CustomSelectorUiState())
    val uiState = _uiState.asStateFlow()

    private val cacheSHA1 = mutableMapOf<imageId, imageSHA>()

    private val allImages = mutableListOf<ImageUiState>()
    private val foldersMap = mutableMapOf<Long, MutableList<Image>>()

    init {
        viewModelScope.launch {
            imageRepository.getImagesFromDevice().collect { image ->
                val bucketId = image.bucketId

                allImages.add(image.toImageUiState())
                foldersMap.getOrPut(bucketId) { mutableListOf() }.add(image)
            }
            val folders = foldersMap.map { (bucketId, images)->
                val firstImage = images.first()
                Folder(
                    bucketId = bucketId,
                    bucketName = firstImage.bucketName,
                    preview = firstImage.uri,
                    itemsCount = images.size,
                    images = images
                )
            }
            _uiState.update { it.copy(isLoading = false, folders = folders) }
        }
    }

    fun onEvent(e: CustomSelectorEvent) {
        when(e) {
            is CustomSelectorEvent.OnFolderClick -> {
                _uiState.update {
                    it.copy(
                        filteredImages = foldersMap[e.bucketId]?.map {
                            img -> img.toImageUiState()
                        } ?: emptyList()
                    )
                }
            }

            is CustomSelectorEvent.OnImageSelection -> {
                _uiState.update { state ->
                    val updatedSelectedIds = if (state.selectedImageIds.contains(e.imageId)) {
                        state.selectedImageIds - e.imageId // Remove if already selected
                    } else {
                        state.selectedImageIds + e.imageId // Add if not selected
                    }
                    state.copy(selectedImageIds = updatedSelectedIds)
                }
            }

            is CustomSelectorEvent.OnDragImageSelection-> {
                _uiState.update { it.copy(selectedImageIds = e.imageIds) }
            }

            CustomSelectorEvent.OnUnselectAll-> {
                _uiState.update { it.copy(selectedImageIds = emptySet()) }
            }
        }
    }
}