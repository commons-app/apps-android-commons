package fr.free.nrw.commons.bookmarks.pictures

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.bookmarks.models.Bookmark
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class BookmarkPicturesRoomDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(bookmark: BookmarkPictureRoomEntity)

    @Delete
    protected abstract fun deleteInternal(bookmark: BookmarkPictureRoomEntity)

    @Query("SELECT * FROM bookmarks")
    protected abstract fun getAllInternal(): Single<List<BookmarkPictureRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks WHERE media_name = :mediaName)")
    abstract fun findBookmarkByName(mediaName: String): Boolean

    fun getAllBookmarks(): List<Bookmark> {
        return getAllInternal().map { entities ->
            entities.map { Bookmark(it.mediaName, it.mediaCreator) }
        }.blockingGet()
    }

    fun findBookmark(bookmark: Bookmark?): Boolean {
        if (bookmark?.mediaName == null) return false
        return findBookmarkByName(bookmark.mediaName!!)
    }

    fun updateBookmark(bookmark: Bookmark): Single<Boolean> {
        return Single.fromCallable {
            val entity = BookmarkPictureRoomEntity(bookmark.mediaName!!, bookmark.mediaCreator!!)
            if (findBookmarkByName(bookmark.mediaName!!)) {
                deleteInternal(entity)
                false
            } else {
                insertInternal(entity)
                true
            }
        }
    }
}
