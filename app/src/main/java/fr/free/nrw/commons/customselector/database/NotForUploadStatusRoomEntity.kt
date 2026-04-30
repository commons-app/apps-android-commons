package fr.free.nrw.commons.customselector.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the images_not_for_upload_table.
 */
@Entity(tableName = "images_not_for_upload_table")
data class NotForUploadStatusRoomEntity(
    @PrimaryKey
    val imageSHA1: String
)
