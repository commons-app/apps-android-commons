package fr.free.nrw.commons.review

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for the reviewed-images table.
 */
@Entity(tableName = "reviewed-images")
data class ReviewRoomEntity(
    @PrimaryKey
    val imageId: String
)
