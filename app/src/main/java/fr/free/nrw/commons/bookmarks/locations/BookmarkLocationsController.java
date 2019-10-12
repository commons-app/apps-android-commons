package fr.free.nrw.commons.bookmarks.locations;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.nearby.Place;

@Singleton
class BookmarkLocationsController {

    @Inject
    BookmarkLocationsDao bookmarkLocationDao;

    @Inject
    public BookmarkLocationsController() {
        // for injector
    }

    /**
     * Load from DB the bookmarked locations
     * @return a list of Place objects.
     */
    List<Place> loadFavoritesLocations() {
        return bookmarkLocationDao.getAllBookmarksLocations();
    }
}
