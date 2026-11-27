package fr.free.nrw.commons.settings.sync

import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holder class grouping all bookmark DAOs required for user settings sync.
 */
@Singleton
class BookmarkDaos
    @Inject
    constructor(
        val locationsDao: BookmarkLocationsDao,
        val categoriesDao: BookmarkCategoriesDao,
        val itemsDao: BookmarkItemsDao,
        val picturesDao: BookmarkPicturesDao,
    )
