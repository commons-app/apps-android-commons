package fr.free.nrw.commons.bookmarks.pictures

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Single

@Dao
interface BookmarkPicturesRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(bookmark: BookmarkPictureRoomEntity)

    @Delete
    fun delete(bookmark: BookmarkPictureRoomEntity)

    @Query("SELECT * FROM bookmarks")
    fun getAll(): Single<List<BookmarkPictureRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks WHERE media_name = :mediaName)")
    fun findBookmarkByName(mediaName: String): Boolean
}
