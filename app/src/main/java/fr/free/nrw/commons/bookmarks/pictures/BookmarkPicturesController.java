package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Observable;
import io.reactivex.ObservableSource;
import io.reactivex.Single;
import io.reactivex.functions.Function;

@Singleton
public class BookmarkPicturesController {

    private final OkHttpJsonApiClient okHttpJsonApiClient;
    private final BookmarkPicturesDao bookmarkDao;

    private List<Bookmark> currentBookmarks;

    @Inject
    public BookmarkPicturesController(OkHttpJsonApiClient okHttpJsonApiClient,
                                      BookmarkPicturesDao bookmarkDao) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.bookmarkDao = bookmarkDao;
        currentBookmarks = new ArrayList<>();
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    Single<List<Media>> loadBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        currentBookmarks = bookmarks;
        return Observable.fromIterable(bookmarks)
                .flatMap((Function<Bookmark, ObservableSource<Media>>) bookmark ->
                        okHttpJsonApiClient.getMedia(bookmark.getMediaName(), false).toObservable())
                .toList();
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    boolean needRefreshBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        return bookmarks.size() != currentBookmarks.size();
    }

    /**
     * Cancels the requests to the API and the DB
     */
    void stop() {
        //noop
    }
}
