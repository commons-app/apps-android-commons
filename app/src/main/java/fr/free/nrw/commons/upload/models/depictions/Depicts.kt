package fr.free.nrw.commons.upload.models.depictions

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

/**
 *  entity class for DepictsRoomDateBase
 */
@Entity(tableName = "depicts_table")
data class Depicts (@PrimaryKey val item: DepictedItem, val lastUsed:Date)