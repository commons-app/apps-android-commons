package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;

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
    List<Media> loadBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        currentBookmarks = bookmarks;
        ArrayList<Media> medias = new ArrayList<>();
        for (Bookmark bookmark : bookmarks) {
            List<Media> tmpMedias = okHttpJsonApiClient
                    .getMediaList("search", bookmark.getMediaName())
                    .blockingGet();
            for (Media m : tmpMedias) {
                if (m.getCreator().trim().equals(bookmark.getMediaCreator().trim())) {
                    medias.add(m);
                    break;
                }
            }
        }
        return medias;
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    boolean needRefreshBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        if (bookmarks.size() == currentBookmarks.size()) {
            return false;
        }
        return true;
    }

    /**
     * Cancels the requests to the API and the DB
     */
    void stop() {
        //noop
    }
}
