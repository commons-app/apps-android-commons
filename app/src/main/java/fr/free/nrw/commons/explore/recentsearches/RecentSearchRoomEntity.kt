package fr.free.nrw.commons.explore.recentsearches

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recent_searches")
data class RecentSearchRoomEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "_id") val id: Long = 0,
    @ColumnInfo(name = "name") val query: String,
    @ColumnInfo(name = "last_used") val lastSearched: Date
)
