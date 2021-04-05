/*
 * Copyright (c) 2020 Nguyen Hoang Lam.
 * All rights reserved.
 */

package com.nguyenhoanglam.imagepicker.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable


data class Image(var id: Long, var name: String, var uri: Uri, var path: String, var bucketId: Long = 0, var bucketName: String = "") : Parcelable {
    constructor(parcel: Parcel) : this(parcel.readLong(), parcel.readString()!!, parcel.readParcelable(Uri::class.java.classLoader)!!, parcel.readString()!!, parcel.readLong(), parcel.readString()!!) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeParcelable(uri, flags)
        parcel.writeString(path)
        parcel.writeLong(bucketId)
        parcel.writeString(bucketName)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Image> {
        override fun createFromParcel(parcel: Parcel): Image {
            return Image(parcel)
        }

        override fun newArray(size: Int): Array<Image?> {
            return arrayOfNulls(size)
        }
    }
}