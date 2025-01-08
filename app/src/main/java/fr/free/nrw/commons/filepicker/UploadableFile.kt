package fr.free.nrw.commons.filepicker

import android.annotation.SuppressLint
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

import androidx.exifinterface.media.ExifInterface

import fr.free.nrw.commons.upload.FileUtils
import fr.free.nrw.commons.upload.ImageCoordinates
import java.io.File
import java.io.IOException
import java.util.Date
import timber.log.Timber

class UploadableFile : Parcelable {

    val contentUri: Uri
    val file: File

    constructor(contentUri: Uri, file: File) {
        this.contentUri = contentUri
        this.file = file
    }

    constructor(file: File) {
        this.file = file
        this.contentUri = Uri.fromFile(File(file.path))
    }

    private constructor(parcel: Parcel) {
        contentUri = parcel.readParcelable(Uri::class.java.classLoader)!!
        file = parcel.readSerializable() as File
    }

    fun getFilePath(): String {
        return file.path
    }

    fun getMediaUri(): Uri {
        return Uri.parse(getFilePath())
    }

    fun getMimeType(context: Context): String? {
        return FileUtils.getMimeType(context, getMediaUri())
    }

    override fun describeContents(): Int = 0

    /**
     * First try to get the file creation date from EXIF, else fall back to Content Provider (CP)
     */
    fun getFileCreatedDate(context: Context): DateTimeWithSource? {
        return getDateTimeFromExif() ?: getFileCreatedDateFromCP(context)
    }

    /**
     * Get filePath creation date from URI using all possible content providers
     */
    private fun getFileCreatedDateFromCP(context: Context): DateTimeWithSource? {
        return try {
            val cursor: Cursor? = context.contentResolver.query(contentUri, null, null, null, null)
            cursor?.use {
                val lastModifiedColumnIndex = cursor
                    .getColumnIndex(
                        "last_modified"
                    ).takeIf { it != -1 }
                    ?: cursor.getColumnIndex("datetaken")
                if (lastModifiedColumnIndex == -1) return null // No valid column found
                cursor.moveToFirst()
                DateTimeWithSource(
                    cursor.getLong(
                        lastModifiedColumnIndex
                    ), DateTimeWithSource.CP_SOURCE)
            }
        } catch (e: Exception) {
            Timber.tag("UploadableFile").d(e)
            null
        }
    }

    /**
     * Indicates whether the EXIF contains the location (both latitude and longitude).
     */
    fun hasLocation(): Boolean {
        return try {
            val exif = ExifInterface(file.absolutePath)
            ImageCoordinates(exif, null).imageCoordsExists
        } catch (e: IOException) {
            Timber.tag("UploadableFile").d(e)
            false
        }
    }

    /**
     * Get filePath creation date from URI using EXIF data
     */
    private fun getDateTimeFromExif(): DateTimeWithSource? {
        return try {
            val exif = ExifInterface(file.absolutePath)
            val dateTimeSubString = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
            if (dateTimeSubString != null) {
                val year = dateTimeSubString.substring(0, 4).toInt()
                val month = dateTimeSubString.substring(5, 7).toInt()
                val day = dateTimeSubString.substring(8, 10).toInt()
                val dateCreatedString = "%04d-%02d-%02d".format(year, month, day)
                if (dateCreatedString.length == 10) {
                    @SuppressLint("RestrictedApi")
                    val dateTime = exif.dateTimeOriginal
                    if (dateTime != null) {
                        val date = Date(dateTime)
                        return DateTimeWithSource(date, dateCreatedString, DateTimeWithSource.EXIF_SOURCE)
                    }
                }
            }
            null
        } catch (e: Exception) {
            Timber.tag("UploadableFile").d(e)
            null
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeParcelable(contentUri, flags)
        parcel.writeSerializable(file)
    }

    class DateTimeWithSource {
        companion object {
            const val CP_SOURCE = "contentProvider"
            const val EXIF_SOURCE = "exif"
        }

        val epochDate: Long
        var dateString: String? = null
        val source: String

        constructor(epochDate: Long, source: String) {
            this.epochDate = epochDate
            this.source = source
        }

        constructor(date: Date, source: String) {
            epochDate = date.time
            this.source = source
        }

        constructor(date: Date, dateString: String, source: String) {
            epochDate = date.time
            this.dateString = dateString
            this.source = source
        }
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
