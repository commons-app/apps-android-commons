package fr.free.nrw.commons.customselector.ui.screens

import fr.free.nrw.commons.customselector.ui.states.ImageUiState
import kotlinx.coroutines.CoroutineScope

sealed interface CustomSelectorEvent {
    data class OnSwitchHandledPictures(val isEnabled: Boolean): CustomSelectorEvent
    data class OnFolderClick(val bucketId: Long): CustomSelectorEvent
    data class OnImageSelection(val imageId: Long): CustomSelectorEvent
    data class OnDragImageSelection(val imageIds: Set<Long>): CustomSelectorEvent
    data object OnUnselectAll: CustomSelectorEvent
    data class OnUpdateImageStatus(val scope: CoroutineScope, val image: ImageUiState) : CustomSelectorEvent
    data object MarkAsNotForUpload: CustomSelectorEvent
    data object UnmarkAsNotForUpload: CustomSelectorEvent
}