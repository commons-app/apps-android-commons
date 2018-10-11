package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class BookmarkPictureListController {

    private MediaWikiApi mediaWikiApi;

    @Inject
    BookmarkPictureDao bookmarkDao;

    @Inject public BookmarkPictureListController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    /**
     * Loads the Media objects from the raw data stored in DB and the API.
     * @return a list of bookmarked Media object
     */
    List<Media> loadBookmarkedPictures() {
        List<Bookmark> bookmarks = bookmarkDao.getAllBookmarks();
        ArrayList<Media> medias = new ArrayList<Media>();
        for (Bookmark bookmark : bookmarks) {
            List<Media> tmpMedias = mediaWikiApi.searchImages(bookmark.getMediaName(), 0);
            if (tmpMedias.size() > 0) {
                medias.add(tmpMedias.get(0));
            }
        }
        return medias;
    }

    /**
     * Cancels the requests to the API and the DB
     */
    void stop() {
        //noop
    }
}
