package fr.free.nrw.commons.bookmarks.category

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkCategoriesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(bookmarksCategoryModal: BookmarksCategoryModal)

    @Delete
    suspend fun delete(bookmarksCategoryModal: BookmarksCategoryModal)

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks_categories WHERE categoryName = :categoryName)")
    suspend fun doesExist(categoryName: String): Boolean

    @Query("SELECT * FROM bookmarks_categories")
    fun getAllCategories(): Flow<List<BookmarksCategoryModal>>

}
