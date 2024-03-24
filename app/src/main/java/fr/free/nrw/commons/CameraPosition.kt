package fr.free.nrw.commons

import android.os.Parcel
import android.os.Parcelable

class CameraPosition(val latitude: Double, val longitude: Double, val zoom: Double) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readDouble(),
        parcel.readDouble(),
        parcel.readDouble()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeDouble(latitude)
        parcel.writeDouble(longitude)
        parcel.writeDouble(zoom)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CameraPosition> {
        override fun createFromParcel(parcel: Parcel): CameraPosition {
            return CameraPosition(parcel)
        }

        override fun newArray(size: Int): Array<CameraPosition?> {
            return arrayOfNulls(size)
        }
    }
}
