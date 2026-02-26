package fr.free.nrw.commons.customselector.ui.states

import android.net.Uri
import fr.free.nrw.commons.customselector.domain.model.Image

data class ImageUiState(
    val id: Long,
    val name: String,
    val uri: Uri,
    val bucketId: Long,
    val isNotForUpload: Boolean = false,
    val isUploaded: Boolean = false
)

fun Image.toImageUiState() = ImageUiState(
    id = id,
    name = name,
    uri = uri,
    bucketId = bucketId
)