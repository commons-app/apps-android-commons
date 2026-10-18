package fr.free.nrw.commons.category

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "categories")
data class CategoryRoomEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "description") val description: String? = null,
    @ColumnInfo(name = "thumbnail") val thumbnail: String? = null,
    @ColumnInfo(name = "last_used") val lastUsed: Date? = null,
    @ColumnInfo(name = "times_used") var timesUsed: Int = 0
)
