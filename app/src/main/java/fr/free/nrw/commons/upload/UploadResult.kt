package fr.free.nrw.commons.upload

import android.os.Parcel
import android.os.Parcelable
import org.wikipedia.gallery.ImageInfo

private const val RESULT_SUCCESS = "Success"


data class UploadResult(
    val result: String,
    val filekey: String,
    val offset: Int,
    var filename: String
) : Parcelable {
    constructor(parcel: Parcel) : this(
        result = parcel.readString() ?: "",
        filekey = parcel.readString() ?: "",
        offset = parcel.readInt() ?: 0,
        filename = parcel.readString() ?: "".trim()
    ) {
    }

    fun isSuccessful(): Boolean = result == RESULT_SUCCESS

    fun createCanonicalFileName() = "File:$filename"
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(result)
        parcel.writeString(filekey)
        parcel.writeInt(offset)
        parcel.writeString(filename.trim())
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UploadResult> {
        override fun createFromParcel(parcel: Parcel): UploadResult {
            return UploadResult(parcel)
        }

        override fun newArray(size: Int): Array<UploadResult?> {
            return arrayOfNulls(size)
        }
    }
}
