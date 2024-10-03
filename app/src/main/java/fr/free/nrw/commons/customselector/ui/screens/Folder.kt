package fr.free.nrw.commons.customselector.ui.screens

import android.net.Uri
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Folder(
    val bucketId: Long,
    val bucketName: String,
    val preview: Uri,
    val itemsCount: Int
): Parcelable
