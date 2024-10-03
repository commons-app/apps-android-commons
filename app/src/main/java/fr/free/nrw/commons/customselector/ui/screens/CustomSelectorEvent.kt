package fr.free.nrw.commons.customselector.ui.screens

interface CustomSelectorEvent {
    data class OnFolderClick(val bucketId: Long): CustomSelectorEvent
    data class OnImageSelect(val imageId: Long): CustomSelectorEvent
}