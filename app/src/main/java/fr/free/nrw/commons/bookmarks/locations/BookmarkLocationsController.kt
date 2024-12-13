package fr.free.nrw.commons.bookmarks.locations

import javax.inject.Inject
import javax.inject.Singleton

import fr.free.nrw.commons.nearby.Place

@Singleton
class BookmarkLocationsController @Inject constructor(
    private val bookmarkLocationDao: BookmarkLocationsDao
) {

    /**
     * Load from DB the bookmarked locations
     * @return a list of Place objects.
     */
    fun loadFavoritesLocations(): List<Place> {
        return bookmarkLocationDao.getAllBookmarksLocations()
    }
}
