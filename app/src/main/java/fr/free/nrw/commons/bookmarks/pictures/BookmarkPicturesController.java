package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class BookmarkPicturesController {

    private MediaWikiApi mediaWikiApi;

    @Inject
    BookmarkPicturesDao bookmarkDao;

    private List<Bookmark> currentBookmarks;

    @Inject public BookmarkPicturesController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
        currentBookmarks = new ArrayList<>();
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    List<Media> loadBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        currentBookmarks = bookmarks;
        ArrayList<Media> medias = new ArrayList<Media>();
        for (Bookmark bookmark : bookmarks) {
            List<Media> tmpMedias = mediaWikiApi.searchImages(bookmark.getMediaName(), 0);
            for (Media m : tmpMedias) {
                if (m.getCreator().equals(bookmark.getMediaCreator())) {
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
