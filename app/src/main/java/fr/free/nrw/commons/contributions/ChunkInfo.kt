package fr.free.nrw.commons.contributions

import android.os.Parcel
import android.os.Parcelable
import fr.free.nrw.commons.upload.UploadResult

data class ChunkInfo(
    val uploadResult: UploadResult,
    val lastChunkIndex: Int,
    var isLastChunkUploaded: Boolean
) : Parcelable {
    constructor(parcel: Parcel) : this(
        parcel.readParcelable(UploadResult::class.java.classLoader),
        parcel.readInt(),
        parcel.readByte() != 0.toByte()
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(uploadResult, flags)
        parcel.writeInt(lastChunkIndex)
        parcel.writeByte(if (isLastChunkUploaded) 1 else 0)
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