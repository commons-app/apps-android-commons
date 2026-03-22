package fr.free.nrw.commons.bookmarks.items

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarksItems")
data class BookmarkItemsRoomEntity(
    @ColumnInfo(name = "item_name") val name: String,
    @ColumnInfo(name = "item_description") val description: String?,
    @ColumnInfo(name = "item_image_url") val imageUrl: String?,
    @ColumnInfo(name = "item_instance_of") val instanceOfs: String,
    @ColumnInfo(name = "item_name_categories") val categoryNames: String,
    @ColumnInfo(name = "item_description_categories") val categoryDescriptions: String,
    @ColumnInfo(name = "item_thumbnail_categories") val categoryThumbnails: String,
    @ColumnInfo(name = "item_is_selected") var isSelected: Boolean,
    @PrimaryKey @ColumnInfo(name = "item_id") val id: String
)