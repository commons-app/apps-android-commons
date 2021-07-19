package fr.free.nrw.commons.bookmarks.items

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkItemsController {

    @Inject
    lateinit var bookmarkItemsDao: BookmarkItemsDao

    @Inject
    fun BookmarkItemsController() {}

    /**
     * Load from DB the bookmarked locations
     * @return a list of Place objects.
     */
    fun loadFavoritesItems(): List<DepictedItem> {
        return bookmarkItemsDao.getAllBookmarksItems()
    }
}