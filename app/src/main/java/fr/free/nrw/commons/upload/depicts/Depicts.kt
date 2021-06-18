package fr.free.nrw.commons.upload.depicts

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import java.util.*

/**
 *  entity class for DepictsRoomDateBase
 */
@Entity(tableName = "depicts_table")
data class Depicts (@PrimaryKey val item: DepictedItem, val lastUsed:Date)