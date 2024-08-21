package fr.free.nrw.commons.upload

data class StashUploadResult(
    val state: StashUploadState,
    val fileKey: String?,
    val errorMessage : String?
)

enum class StashUploadState {
    SUCCESS,
    PAUSED,
    FAILED
}
