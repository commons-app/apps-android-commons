package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.bookmarks.Bookmark;
import fr.free.nrw.commons.bookmarks.BookmarkDao;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class BookmarkPictureListController {

    private MediaWikiApi mediaWikiApi;

    @Inject
    BookmarkDao bookmarkDao;

    @Inject public BookmarkPictureListController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    List<Media> loadBookmarkedPictures() {
        //return mediaWikiApi.searchImages("th", 0);
        // TODO Use Dao to load
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

    void stop() {
        // TODO Cancel all requests
    }
}
