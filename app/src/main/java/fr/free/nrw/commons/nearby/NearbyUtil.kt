package fr.free.nrw.commons.nearby

import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarksLocations
import kotlinx.coroutines.launch

object NearbyUtil {

    fun getBookmarkLocationExists(
        bookmarksLocationsDao: BookmarkLocationsDao,
        name: String,
        scope: LifecycleCoroutineScope?
    ): Boolean {
        var isBookmarked = false
        scope?.launch {
            isBookmarked = bookmarksLocationsDao.findBookmarkLocation(name)
        }

        return isBookmarked
    }
}