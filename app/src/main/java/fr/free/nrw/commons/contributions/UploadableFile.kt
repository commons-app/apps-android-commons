package fr.free.nrw.commons.contributions

import android.os.Parcel
import android.os.Parcelable

class UploadableFile(val filePath: String,
                     val mimeType: String) : Parcelable {
    constructor(parcel: Parcel) : this(
            parcel.readString(),
            parcel.readString()) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(filePath)
        parcel.writeString(mimeType)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<UploadableFile> {
        override fun createFromParcel(parcel: Parcel): UploadableFile {
            return UploadableFile(parcel)
        }

        override fun newArray(size: Int): Array<UploadableFile?> {
            return arrayOfNulls(size)
        }
    }
}
