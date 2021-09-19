package fr.free.nrw.commons.customselector.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.*

/**
 * Entity class for Uploaded Status.
 */
@Entity(tableName = "uploaded_table", indices = [Index(value = ["modifiedImageSHA1"], unique = true)])
data class UploadedStatus(

    /**
     * Original image sha1.
     */
    @PrimaryKey
    val imageSHA1 : String,

    /**
     * Modified image sha1 (after exif changes).
     */
    val modifiedImageSHA1 : String,

    /**
     * imageSHA1 query result from API.
     */
    var imageResult : Boolean,

    /**
     * modifiedImageSHA1 query result from API.
     */
    var modifiedImageResult : Boolean,

    /**
     * lastUpdated for data validation.
     */
    var lastUpdated : Date? = null
)
