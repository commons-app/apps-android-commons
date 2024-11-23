package fr.free.nrw.commons.review

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store reviewed/skipped images identifier
 */
@Entity(tableName = "reviewed-images")
data class ReviewEntity(
    @PrimaryKey
    val imageId: String
)
