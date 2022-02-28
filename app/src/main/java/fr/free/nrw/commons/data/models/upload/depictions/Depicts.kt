package fr.free.nrw.commons.data.models.upload.depictions

import androidx.room.Entity
import androidx.room.PrimaryKey
import fr.free.nrw.commons.data.models.upload.depictions.DepictedItem
import java.util.*

/**
 *  entity class for DepictsRoomDateBase
 */
@Entity(tableName = "depicts_table")
data class Depicts (@PrimaryKey val item: DepictedItem, val lastUsed:Date)