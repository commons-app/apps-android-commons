package fr.free.nrw.commons.customselector.data

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import android.text.format.DateFormat
import fr.free.nrw.commons.customselector.domain.model.Image
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.util.Calendar
import java.util.Date
import javax.inject.Inject

class MediaReader @Inject constructor(private val context: Context) {
    fun getImages() = flow {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.MIME_TYPE
        )
        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection,
            null, null, MediaStore.Images.Media.DATE_ADDED + " DESC"
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndex(MediaStore.Images.Media._ID)
            val nameColumn = cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA)
            val bucketIdColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)
            val mimeTypeColumn = cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE)

            while(cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val path = cursor.getString(dataColumn)
                val bucketId = cursor.getLong(bucketIdColumn)
                val bucketName = cursor.getString(bucketNameColumn)
                val date = cursor.getLong(dateColumn)
                val mimeType = cursor.getString(mimeTypeColumn)

                val validMimeTypes = arrayOf(
                    "image/jpeg", "image/png", "image/svg+xml", "image/gif",
                    "image/tiff", "image/webp", "image/x-xcf"
                )
                // Skip the media items with unsupported MIME types
                if(mimeType.lowercase() !in validMimeTypes) continue

                // URI to access the image
                val uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id
                )

                val calendar = Calendar.getInstance()
                calendar.timeInMillis = date * 1000L
                val calendarDate: Date = calendar.time
                val dateFormat = DateFormat.getMediumDateFormat(context)
                val formattedDate = dateFormat.format(calendarDate)

                emit(Image(id, name, uri, path, bucketId, bucketName, date = formattedDate))
            }
        }
    }.flowOn(Dispatchers.IO)
}