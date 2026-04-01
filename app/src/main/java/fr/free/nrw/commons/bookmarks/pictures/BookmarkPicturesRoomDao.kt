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
    protected abstract fun insertInternal(bookmark: BookmarkPictureRoomEntity): Completable

    @Delete
    protected abstract fun deleteInternal(bookmark: BookmarkPictureRoomEntity): Completable

    @Query("SELECT * FROM bookmarks")
    protected abstract fun getAllInternal(): Single<List<BookmarkPictureRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks WHERE media_name = :mediaName)")
    abstract fun findBookmarkByName(mediaName: String): Single<Boolean>

    fun getAllBookmarks(): Single<List<Bookmark>> {
        return getAllInternal().map { entities ->
            entities.map { Bookmark(it.mediaName, it.mediaCreator) }
        }
    }

    fun findBookmark(bookmark: Bookmark?): Single<Boolean> {
        if (bookmark?.mediaName == null) return Single.just(false)
        return findBookmarkByName(bookmark.mediaName!!)
    }

    fun updateBookmark(bookmark: Bookmark): Single<Boolean> {
        val entity = BookmarkPictureRoomEntity(bookmark.mediaName!!, bookmark.mediaCreator!!)
        return findBookmarkByName(bookmark.mediaName!!).flatMap { exists ->
            if (exists) {
                deleteInternal(entity).andThen(Single.just(false))
            } else {
                insertInternal(entity).andThen(Single.just(true))
            }
        }
    }
}
