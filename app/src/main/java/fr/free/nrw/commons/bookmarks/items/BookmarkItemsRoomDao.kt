package fr.free.nrw.commons.bookmarks.items

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Single

@Dao
interface BookmarkItemsRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(depictedItem: BookmarkItemsRoomEntity)

    @Delete
    fun delete(depictedItem: BookmarkItemsRoomEntity)

    @Query("SELECT * FROM bookmarksItems")
    fun getAll(): Single<List<BookmarkItemsRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarksItems WHERE item_id = :itemId)")
    fun findBookmarkItem(itemId: String?): Boolean
}
