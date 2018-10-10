package fr.free.nrw.commons.bookmarks.pictures;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class BookmarkPictureListController {

    private MediaWikiApi mediaWikiApi;

    @Inject public BookmarkPictureListController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    List<Media> loadBookmarkedPictures() {
        return mediaWikiApi.searchImages("th", 0);
        // TODO Use Dao to load
        //return new ArrayList<>();
    }

    void stop() {
        // TODO Cancel all requests
    }
}
