package fr.free.nrw.commons.bookmarks.locations;

import fr.free.nrw.commons.nearby.Place;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class BookmarkLocationsController {

  @Inject
  BookmarkLocationsDao bookmarkLocationDao;

  @Inject
  public BookmarkLocationsController() {
  }

  /**
   * Load from DB the bookmarked locations
   *
   * @return a list of Place objects.
   */
  public List<Place> loadFavoritesLocations() {
    return bookmarkLocationDao.getAllBookmarksLocations();
  }
}
