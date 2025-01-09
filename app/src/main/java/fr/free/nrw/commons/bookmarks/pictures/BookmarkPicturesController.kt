package fr.free.nrw.commons.bookmarks.pictures

import fr.free.nrw.commons.Media
import fr.free.nrw.commons.bookmarks.models.Bookmark
import fr.free.nrw.commons.media.MediaClient
import io.reactivex.Observable
import io.reactivex.Single
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class BookmarkPicturesController @Inject constructor(
    private val mediaClient: MediaClient,
    private val bookmarkDao: BookmarkPicturesDao
) {

    private var currentBookmarks: List<Bookmark> = emptyList()

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    fun loadBookmarkedPictures(): Single<List<Media>> {
        val bookmarks = bookmarkDao.getAllBookmarks()
        currentBookmarks = bookmarks
        return Observable.fromIterable(bookmarks)
            .flatMap { bookmark -> getMediaFromBookmark(bookmark) }
            .toList()
    }

    private fun getMediaFromBookmark(bookmark: Bookmark): Observable<Media> {
        return mediaClient.getMedia(bookmark.mediaName)
            .toObservable()
            .onErrorResumeNext(Observable.empty())
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    fun needRefreshBookmarkedPictures(): Boolean {
        val bookmarks = bookmarkDao.getAllBookmarks()
        return bookmarks.size != currentBookmarks.size
    }

    /**
     * Cancels the requests to the API and the DB
     */
    fun stop() {
        // noop
    }
}
