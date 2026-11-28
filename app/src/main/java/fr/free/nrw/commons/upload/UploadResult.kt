package fr.free.nrw.commons.upload

import android.os.Parcel
import android.os.Parcelable
import com.google.gson.annotations.SerializedName

private const val RESULT_SUCCESS = "Success"

data class UploadResult(
    val result: String,
    val filekey: String,
    val offset: Int,
    val filename: String,
    @SerializedName("imageinfo") val imageInfo: ImageInfo? = null,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readString() ?: "",
        parcel.readString() ?: "",
        parcel.readInt(),
        parcel.readString() ?: "",
        parcel.readString()?.let(::ImageInfo),
    )

    /** File information included in the MediaWiki upload response. */
    data class ImageInfo(
        @SerializedName("sha1") val sha1: String?,
    )

    fun isSuccessful(): Boolean = result == RESULT_SUCCESS

    fun createCanonicalFileName() = "File:$filename"

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeString(result)
        parcel.writeString(filekey)
        parcel.writeInt(offset)
        parcel.writeString(filename)
        parcel.writeString(imageInfo?.sha1)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<UploadResult> {
        override fun createFromParcel(parcel: Parcel): UploadResult = UploadResult(parcel)

        override fun newArray(size: Int): Array<UploadResult?> = arrayOfNulls(size)
    }
}
