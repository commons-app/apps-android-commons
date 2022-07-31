package fr.free.nrw.commons.customselector.model

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Custom selector data class Image.
 */
data class Image(
    /**
    id : Unique image id, primary key of image in device, eg 104950
     */
    var id: Long,

    /**
    name : Name of the image with extension, eg CommonsLogo.jpeg
     */
    var name: String,

    /**
    uri : Uri of the image, points to image location or name, eg content://media/external/images/camera/10495 (Android 10)
     */
    var uri: Uri,

    /**
    path : System path of the image, eg storage/emulated/0/camera/CommonsLogo.jpeg
     */
    var path: String,

    /**
    bucketId : bucketId of folder, eg 540528482
     */
    var bucketId: Long = 0,

    /**
    bucketName : name of folder, eg Camera
     */
    var bucketName: String = "",

    /**
    sha1 : sha1 of original image.
     */
    var sha1: String = "",

    /**
     * date: Addition date of the image
     */
    var date: String = ""

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
        parcel.writeString(date)
    }

    /**
     * Describe the kinds of special objects contained in this Parcelable
     */
    override fun describeContents(): Int {
        return 0
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {

        if(javaClass != other?.javaClass) {
            return false
        }

        other as Image

        if(id != other.id) {
            return false;
        }
        if(name != other.name) {
            return false;
        }
        if(uri != other.uri) {
            return false;
        }
        if(path != other.path) {
            return false;
        }
        if(bucketId != other.bucketId) {
            return false;
        }
        if(bucketName != other.bucketName) {
            return false;
        }
        if(sha1 != other.sha1) {
            return false;
        }

        return true
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