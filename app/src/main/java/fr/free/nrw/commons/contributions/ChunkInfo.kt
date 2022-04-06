package fr.free.nrw.commons.contributions

import android.os.Parcel
import android.os.Parcelable
import fr.free.nrw.commons.upload.models.UploadResult

data class ChunkInfo(
    val uploadResult: UploadResult?,
    val indexOfNextChunkToUpload: Int,
    val totalChunks: Int
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(UploadResult::class.java.classLoader),
        parcel.readInt(),
        parcel.readInt()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uploadResult, flags)
        parcel.writeInt(indexOfNextChunkToUpload)
        parcel.writeInt(totalChunks)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<ChunkInfo> {
        override fun createFromParcel(parcel: Parcel): ChunkInfo {
            return ChunkInfo(parcel)
        }

        override fun newArray(size: Int): Array<ChunkInfo?> {
            return arrayOfNulls(size)
        }
    }
}