package fr.free.nrw.commons.bookmarks.category

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Bookmark categories dao
 *
 * @constructor Create empty Bookmark categories dao
 */
@Dao
interface BookmarkCategoriesDao {

    /**
     * Insert or Delete category bookmark into DB
     *
     * @param bookmarksCategoryModal
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmarksCategoryModal: BookmarksCategoryModal)


    /**
     * Delete category bookmark from DB
     *
     * @param bookmarksCategoryModal
     */
    @Delete
    suspend fun delete(bookmarksCategoryModal: BookmarksCategoryModal)

    /**
     * Checks if given category exist in DB
     *
     * @param categoryName
     * @return
     */
    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks_categories WHERE categoryName = :categoryName)")
    suspend fun doesExist(categoryName: String): Boolean

    /**
     * Get all categories
     *
     * @return
     */
    @Query("SELECT * FROM bookmarks_categories")
    fun getAllCategories(): Flow<List<BookmarksCategoryModal>>

}
