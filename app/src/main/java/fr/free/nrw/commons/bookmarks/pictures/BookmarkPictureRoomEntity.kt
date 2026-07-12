package fr.free.nrw.commons.bookmarks.pictures

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks")
data class BookmarkPictureRoomEntity(
    @PrimaryKey @ColumnInfo(name = "media_name") val mediaName: String,
    @ColumnInfo(name = "media_creator") val mediaCreator: String?
)
