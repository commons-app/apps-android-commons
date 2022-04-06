package fr.free.nrw.commons.upload.models

data class StashUploadResult(
    val state: StashUploadState,
    val fileKey: String?
)

enum class StashUploadState {
    SUCCESS,
    PAUSED,
    FAILED
}
