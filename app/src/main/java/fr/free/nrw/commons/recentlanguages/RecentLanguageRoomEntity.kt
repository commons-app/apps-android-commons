package fr.free.nrw.commons.recentlanguages

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "recent_languages")
data class RecentLanguageRoomEntity(
    @ColumnInfo(name = "language_name") val languageName: String,
    @PrimaryKey @ColumnInfo(name = "language_code") val languageCode: String
)
