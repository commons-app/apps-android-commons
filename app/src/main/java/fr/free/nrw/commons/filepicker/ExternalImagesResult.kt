package fr.free.nrw.commons.filepicker

/**
 * Files received from another app, ready to be uploaded.
 *
 * @property files The files that can be uploaded
 * @property skippedCount The number of shared files that were left out because Commons does not
 * accept their format
 */
data class ExternalImagesResult(
    val files: List<UploadableFile>,
    val skippedCount: Int,
)
