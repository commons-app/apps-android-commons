package fr.free.nrw.commons.customselector.ui.screens

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
import kotlinx.coroutines.flow.filter
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
    private var openedBucketId: Long? = null

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
            is CustomSelectorEvent.OnSwitchHandledPictures -> {
                val images = if(e.isEnabled) {
                    foldersMap[openedBucketId]?.map { it.toImageUiState() } ?: emptyList()
                } else {
                    _uiState.value.filteredImages.filter { !(it.isNotForUpload || it.isUploaded) }
                }
                _uiState.update { currentState ->
                    currentState.copy(
                        shouldShowHandledPictures = !currentState.shouldShowHandledPictures,
                        filteredImages = images
                    )
                }
            }

            is CustomSelectorEvent.OnFolderClick -> {
                openedBucketId = e.bucketId
                val images = foldersMap[e.bucketId]?.map { it.toImageUiState() } ?: emptyList()
                _uiState.update {
                    it.copy(
                        filteredImages = images
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

            is CustomSelectorEvent.OnUpdateImageStatus -> {
                e.scope.launch { updateNotForUploadStatus(e.image) }
            }

            is CustomSelectorEvent.MarkAsNotForUpload -> {
                viewModelScope.launch {
                    val selectedImageIds = _uiState.value.selectedImageIds

                    val selectedImages = allImages.filter { image ->
                        selectedImageIds.contains(image.id)
                    }

                    selectedImages.forEach { image ->
                        cacheSHA1[image.id]?.let { sha ->
                            if(!imageRepository.isNotForUpload(sha)) {
                                imageRepository.markAsNotForUpload(sha)
                                updateImageStatus(true, image.id)
                                _uiState.update { it.copy(selectedImageIds = emptySet()) }
                            }
                        }
                    }
                }
            }

            CustomSelectorEvent.UnmarkAsNotForUpload -> {
                viewModelScope.launch {
                    val selectedImageIds = _uiState.value.selectedImageIds

                    val selectedImages = allImages.filter { image ->
                        selectedImageIds.contains(image.id)
                    }

                    selectedImages.forEach { image ->
                        cacheSHA1[image.id]?.let { sha ->
                            if(imageRepository.isNotForUpload(sha)) {
                                imageRepository.unmarkAsNotForUpload(sha)
                                updateImageStatus(false, image.id)
                                _uiState.update { it.copy(selectedImageIds = emptySet()) }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun updateImageStatus(isNotForUpload: Boolean, imageId: Long) {
        _uiState.update { state ->
            val updatedImages = state.filteredImages.map {
                if (it.id == imageId) {
                    it.copy(isNotForUpload = isNotForUpload)
                } else {
                    it
                }
            }
            val updateMap = state.imagesNotForUpload.toMutableMap()
            updateMap[imageId] = isNotForUpload

            state.copy(filteredImages = updatedImages, imagesNotForUpload = updateMap)
        }
    }

    private suspend fun updateNotForUploadStatus(image: ImageUiState) {
        val imageSHA = cacheSHA1.getOrPut(image.id) {
            imageUseCase.getImageSHA1(image.uri).also { sha -> cacheSHA1[image.id] = sha }
        }

        val isNotForUpload = imageRepository.isNotForUpload(imageSHA)
        updateImageStatus(isNotForUpload, image.id)
    }
}