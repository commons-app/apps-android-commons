package fr.free.nrw.commons.bookmarks.locations;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.nearby.Place;

@Singleton
public class BookmarkLocationListController {

    @Inject BookmarkLocationDao bookmarkLocationDao;

    @Inject
    public BookmarkLocationListController() {}

    /**
     * Load from DB the bookmarked locations
     * @return a list of Place objects.
     */
    public List<Place> loadFavoritesLocations() {
        return bookmarkLocationDao.getAllBookmarksLocations();
    }
}
