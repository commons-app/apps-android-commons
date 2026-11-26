package fr.free.nrw.commons.upload

/**
 * @param retryAfterMillis how long the server asked to wait before trying again, if it failed
 */
data class StashUploadResult(
    val state: StashUploadState,
    val fileKey: String?,
    val errorMessage: String?,
    val retryAfterMillis: Long? = null,
)

enum class StashUploadState {
    SUCCESS,
    PAUSED,
    FAILED,
    CANCELLED,
}
