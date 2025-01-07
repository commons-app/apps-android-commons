package fr.free.nrw.commons.bookmarks.items

import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles loading bookmarked items from Database
 */
@Singleton
class BookmarkItemsController @Inject constructor(
    private val bookmarkItemsDao: BookmarkItemsDao
) {

    /**
     * Load from DB the bookmarked items
     * @return a list of DepictedItem objects.
     */
    fun loadFavoritesItems(): List<DepictedItem> {
        return bookmarkItemsDao.getAllBookmarksItems()
    }
}