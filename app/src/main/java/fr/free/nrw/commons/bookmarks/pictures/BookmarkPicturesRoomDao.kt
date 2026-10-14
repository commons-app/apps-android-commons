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
    abstract fun insert(bookmark: BookmarkPictureRoomEntity): Completable

    @Delete
    abstract fun delete(bookmark: BookmarkPictureRoomEntity): Completable

    @Query("SELECT * FROM bookmarks")
    abstract fun getAll(): Single<List<BookmarkPictureRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks WHERE media_name = :mediaName)")
    abstract fun findBookmarkByName(mediaName: String): Single<Boolean>

    fun getAllBookmarks(): Single<List<Bookmark>> {
        return getAll().map { entities ->
            entities.map { fromEntity(it) }
        }
    }

    fun findBookmark(bookmark: Bookmark?): Single<Boolean> {
        if (bookmark?.mediaName == null) return Single.just(false)
        return findBookmarkByName(bookmark.mediaName!!)
    }

    fun updateBookmark(bookmark: Bookmark): Single<Boolean> {
        val entity = toEntity(bookmark)
        return findBookmarkByName(bookmark.mediaName!!).flatMap { exists ->
            if (exists) {
                delete(entity).andThen(Single.just(false))
            } else {
                insert(entity).andThen(Single.just(true))
            }
        }
    }

    private fun toEntity(bookmark: Bookmark): BookmarkPictureRoomEntity {
        return BookmarkPictureRoomEntity(bookmark.mediaName!!, bookmark.mediaCreator!!)
    }

    private fun fromEntity(entity: BookmarkPictureRoomEntity): Bookmark {
        return Bookmark(entity.mediaName, entity.mediaCreator)
    }
}
