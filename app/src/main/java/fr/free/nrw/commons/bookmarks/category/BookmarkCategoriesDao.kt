package fr.free.nrw.commons.bookmarks.category

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Bookmark categories dao
 *
 * @constructor Create empty Bookmark categories dao
 */
@Dao
abstract class BookmarkCategoriesDao {

    /**
     * Insert or Delete category bookmark into DB
     *
     * @param bookmarksCategoryModal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertInternal(bookmarkCategoryRoomEntity: BookmarkCategoryRoomEntity)

    suspend fun insert(bookmarksCategoryModal: BookmarksCategoryModal) {
        insertInternal(toEntity(bookmarksCategoryModal))
    }

    /**
     * Delete category bookmark from DB
     *
     * @param bookmarksCategoryModal
     */
    @Delete
    protected abstract suspend fun deleteInternal(bookmarkCategoryRoomEntity: BookmarkCategoryRoomEntity)

    suspend fun delete(bookmarksCategoryModal: BookmarksCategoryModal) {
        deleteInternal(toEntity(bookmarksCategoryModal))
    }

    /**
     * Checks if given category exist in DB
     *
     * @param categoryName
     * @return
     */
    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks_categories WHERE categoryName = :categoryName)")
    abstract suspend fun doesExist(categoryName: String): Boolean

    /**
     * Get all categories
     *
     * @return
     */
    @Query("SELECT * FROM bookmarks_categories")
    protected abstract fun getAllCategoriesInternal(): Flow<List<BookmarkCategoryRoomEntity>>

    fun getAllCategories(): Flow<List<BookmarksCategoryModal>> =
        getAllCategoriesInternal().map { entities -> entities.map { fromEntity(it) } }

    private fun toEntity(model: BookmarksCategoryModal): BookmarkCategoryRoomEntity =
        BookmarkCategoryRoomEntity(categoryName = model.categoryName)

    private fun fromEntity(entity: BookmarkCategoryRoomEntity): BookmarksCategoryModal =
        BookmarksCategoryModal(categoryName = entity.categoryName)
}
