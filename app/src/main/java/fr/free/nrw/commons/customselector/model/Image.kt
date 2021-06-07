package fr.free.nrw.commons.customselector.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

data class Image(
    /**
    id : Unique image id
     */
    var id: Long,

    /**
    name : Name of the image
     */
    var name: String,

    /**
    uri : Uri of the image
     */
    var uri: Uri,

    /**
    path : System path of the image
     */
    var path: String,

    /**
    bucketId : bucketId of folder
     */
    var bucketId: Long = 0,

    /**
    bucketName : name of folder
     */
    var bucketName: String = "",

    /**
    sha1 : sha1 of original image.
     */
    var sha1: String = ""
) : Parcelable {

    /**
    default parcelable constructor.
     */
    constructor(parcel: Parcel):
            this(parcel.readLong(),
                parcel.readString()!!,
                parcel.readParcelable(Uri::class.java.classLoader)!!,
                parcel.readString()!!,
                parcel.readLong(),
                parcel.readString()!!,
                parcel.readString()!!
            )

    /**
    Write to parcel method.
     */
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(name)
        parcel.writeParcelable(uri, flags)
        parcel.writeString(path)
        parcel.writeLong(bucketId)
        parcel.writeString(bucketName)
        parcel.writeString(sha1)
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Parcelable companion object
     */
    companion object CREATOR : Parcelable.Creator<Image> {
        override fun createFromParcel(parcel: Parcel): Image {
            return Image(parcel)
        }

        override fun newArray(size: Int): Array<Image?> {
            return arrayOfNulls(size)
        }
    }
}