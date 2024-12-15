package fr.free.nrw.commons.bookmarks.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bookmarks_categories")
data class BookmarksCategoryModal(
    @PrimaryKey val categoryName: String
)