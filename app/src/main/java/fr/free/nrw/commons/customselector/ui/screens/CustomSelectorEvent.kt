package fr.free.nrw.commons.customselector.ui.screens

interface CustomSelectorEvent {
    data class OnFolderClick(val bucketId: Long): CustomSelectorEvent
    data class OnImageSelection(val imageId: Long): CustomSelectorEvent
    data class OnDragImageSelection(val imageIds: Set<Long>): CustomSelectorEvent
    data object OnUnselectAll: CustomSelectorEvent
}