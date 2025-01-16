package fr.free.nrw.commons.bookmarks.locations

import fr.free.nrw.commons.nearby.Place
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BookmarkLocationsController @Inject constructor(
    private val bookmarkLocationDao: BookmarkLocationsDao
) {

    /**
     * Load bookmarked locations from the database.
     * @return a list of Place objects.
     */
    fun loadFavoritesLocations(): List<Place> = bookmarkLocationDao.getAllBookmarksLocations()
}
