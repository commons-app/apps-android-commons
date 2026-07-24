package fr.free.nrw.commons.upload.depicts

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.Date

/**
 * Room entity for the depicts_table.
 */
@Entity(tableName = "depicts_table")
data class DepictsRoomEntity(
    @PrimaryKey val item: DepictedItem,
    val lastUsed: Date
)
