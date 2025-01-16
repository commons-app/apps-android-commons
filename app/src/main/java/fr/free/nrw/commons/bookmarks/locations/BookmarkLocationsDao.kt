package fr.free.nrw.commons.bookmarks.locations

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Dao
abstract class BookmarkLocationsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addBookmarkLocation(bookmarkLocation: BookmarksLocations)

    @Query("SELECT * FROM bookmarks_locations")
    abstract suspend fun getAllBookmarksLocations(): List<BookmarksLocations>

    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks_locations WHERE location_name = :name)")
    abstract suspend fun findBookmarkLocation(name: String): Boolean

    @Delete
    abstract suspend fun deleteBookmarkLocation(bookmarkLocation: BookmarksLocations)

    suspend fun updateBookmarkLocation(bookmarkLocation: Place): Boolean {
        val bookmarkLocationExists = findBookmarkLocation(bookmarkLocation.name)

        if(bookmarkLocationExists) {
            deleteBookmarkLocation(
                bookmarkLocation.toBookmarksLocations()
            )
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, false)
        } else {
            addBookmarkLocation(
                bookmarkLocation.toBookmarksLocations()
            )
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, true)
        }

        return !bookmarkLocationExists
    }

    fun getAllBookmarksLocationsPlace(): Flow<List<Place>> {
        return flow { getAllBookmarksLocations().map { it.toPlace() } }
    }
}