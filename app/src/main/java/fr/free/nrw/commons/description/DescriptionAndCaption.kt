package fr.free.nrw.commons.description

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class DescriptionAndCaption(
    val language: String, val description: String, val caption: String) : Parcelable