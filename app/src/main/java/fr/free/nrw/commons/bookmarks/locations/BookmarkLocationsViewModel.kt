package fr.free.nrw.commons.bookmarks.locations

import androidx.lifecycle.ViewModel
import fr.free.nrw.commons.nearby.Place
import kotlinx.coroutines.flow.Flow

class BookmarkLocationsViewModel(
    @Suppress("UnusedPrivateProperty") // Will be used when getAllBookmarkLocations is enabled
    private val bookmarkLocationsDao: BookmarkLocationsDao
): ViewModel() {

//    fun getAllBookmarkLocations(): List<Place> {
//        return bookmarkLocationsDao.getAllBookmarksLocationsPlace()
//    }

}
