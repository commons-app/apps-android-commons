package fr.free.nrw.commons.bookmarks.items;

import fr.free.nrw.commons.upload.models.depictions.DepictedItem;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Handles loading bookmarked items from Database
 */
@Singleton
public class BookmarkItemsController {

    @Inject
    BookmarkItemsDao bookmarkItemsDao;

    @Inject
    public BookmarkItemsController() {}

    /**
     * Load from DB the bookmarked items
     * @return a list of DepictedItem objects.
     */
    public List<DepictedItem> loadFavoritesItems() {
        return bookmarkItemsDao.getAllBookmarksItems();
    }
}
