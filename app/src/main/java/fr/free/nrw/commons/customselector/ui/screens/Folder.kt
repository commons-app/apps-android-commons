package fr.free.nrw.commons.customselector.ui.screens

import android.net.Uri
import android.os.Parcelable
import fr.free.nrw.commons.customselector.domain.model.Image
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val bucketId: Long,
    val bucketName: String,
    val preview: Uri,
    val images: List<Image>,
    val itemsCount: Int
): Parcelable
