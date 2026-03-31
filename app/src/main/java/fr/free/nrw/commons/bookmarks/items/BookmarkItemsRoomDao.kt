package fr.free.nrw.commons.bookmarks.items

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.category.CategoryItem
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import fr.free.nrw.commons.utils.arrayToString
import fr.free.nrw.commons.utils.stringToArray
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class BookmarkItemsRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(depictedItem: BookmarkItemsRoomEntity)

    @Delete
    protected abstract fun deleteInternal(depictedItem: BookmarkItemsRoomEntity)

    @Query("SELECT * FROM bookmarksItems")
    protected abstract fun getAllInternal(): Single<List<BookmarkItemsRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarksItems WHERE item_id = :itemId)")
    abstract fun findBookmarkItem(itemId: String?): Boolean

    fun getAllBookmarksItems(): Single<List<DepictedItem>> {
        return getAllInternal().map { entities ->
            entities.map { fromEntity(it) }
        }
    }

    fun updateBookmarkItem(depictedItem: DepictedItem): Single<Boolean> {
        return Single.fromCallable {
            if (findBookmarkItem(depictedItem.id)) {
                deleteInternal(toEntity(depictedItem))
                false
            } else {
                insertInternal(toEntity(depictedItem))
                true
            }
        }
    }

    private fun fromEntity(entity: BookmarkItemsRoomEntity): DepictedItem {
        return DepictedItem(
            entity.name,
            entity.description,
            entity.imageUrl,
            stringToArray(entity.instanceOfs),
            convertToCategoryItems(
                stringToArray(entity.categoryNames),
                stringToArray(entity.categoryDescriptions),
                stringToArray(entity.categoryThumbnails)
            ),
            entity.isSelected,
            entity.id
        )
    }

    private fun toEntity(depictedItem: DepictedItem): BookmarkItemsRoomEntity {
        return BookmarkItemsRoomEntity(
            depictedItem.name,
            depictedItem.description,
            depictedItem.imageUrl,
            arrayToString(depictedItem.instanceOfs) ?: "",
            arrayToString(depictedItem.commonsCategories.map { it.name }) ?: "",
            arrayToString(depictedItem.commonsCategories.map { it.description ?: "" }) ?: "",
            arrayToString(depictedItem.commonsCategories.map { it.thumbnail ?: "" }) ?: "",
            depictedItem.isSelected,
            depictedItem.id
        )
    }

    private fun convertToCategoryItems(
        categoryNameList: List<String>,
        categoryDescriptionList: List<String>,
        categoryThumbnailList: List<String>
    ): List<CategoryItem> = categoryNameList.mapIndexed { index, name ->
        CategoryItem(
            name = name,
            description = categoryDescriptionList.getOrNull(index),
            thumbnail = categoryThumbnailList.getOrNull(index),
            isSelected = false
        )
    }
}
