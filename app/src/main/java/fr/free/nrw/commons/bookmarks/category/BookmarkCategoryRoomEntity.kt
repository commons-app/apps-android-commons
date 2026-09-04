package fr.free.nrw.commons.bookmarks.category

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for bookmarked category in DB
 */
@Entity(tableName = "bookmarks_categories")
data class BookmarkCategoryRoomEntity(
    @PrimaryKey val categoryName: String
)
