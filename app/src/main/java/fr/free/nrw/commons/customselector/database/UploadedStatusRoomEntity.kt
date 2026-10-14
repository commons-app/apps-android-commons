package fr.free.nrw.commons.customselector.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Room entity for the uploaded_table.
 */
@Entity(tableName = "uploaded_table", indices = [Index(value = ["modifiedImageSHA1"], unique = true)])
data class UploadedStatusRoomEntity(
    @PrimaryKey
    val imageSHA1: String,
    val modifiedImageSHA1: String,
    var imageResult: Boolean,
    var modifiedImageResult: Boolean,
    var lastUpdated: Date? = null
)
