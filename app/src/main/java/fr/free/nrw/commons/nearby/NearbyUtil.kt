package fr.free.nrw.commons.nearby

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import fr.free.nrw.commons.R
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import kotlinx.coroutines.launch
import timber.log.Timber

object NearbyUtil {

    fun getBookmarkLocationExists(
        bookmarksLocationsDao: BookmarkLocationsDao,
        name: String,
        scope: LifecycleCoroutineScope?,
        bottomSheetAdapter: BottomSheetAdapter,
    ) {
        scope?.launch {
            val isBookmarked = bookmarksLocationsDao.findBookmarkLocation(name)
            Timber.i("isBookmarked: $isBookmarked")
            if (isBookmarked) {
                bottomSheetAdapter.updateBookmarkIcon(R.drawable.ic_round_star_filled_24px)
            } else {
                bottomSheetAdapter.updateBookmarkIcon(R.drawable.ic_round_star_border_24px)
            }
        }
    }
}