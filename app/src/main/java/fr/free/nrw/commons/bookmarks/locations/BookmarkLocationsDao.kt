package fr.free.nrw.commons.bookmarks.locations

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.nearby.NearbyController
import fr.free.nrw.commons.nearby.Place

/**
 * DAO for managing bookmark locations in the database.
 */
@Dao
abstract class BookmarkLocationsDao {

    /**
     * Adds or updates a bookmark location in the database.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun addBookmarkLocation(bookmarkLocation: BookmarksLocations)

    /**
     * Fetches all bookmark locations from the database.
     */
    @Query("SELECT * FROM bookmarks_locations")
    abstract suspend fun getAllBookmarksLocations(): List<BookmarksLocations>

    /**
     * Checks if a bookmark location exists by name.
     */
    @Query("SELECT EXISTS (SELECT 1 FROM bookmarks_locations WHERE location_name = :name)")
    abstract suspend fun findBookmarkLocation(name: String): Boolean

    /**
     * Deletes a bookmark location from the database.
     */
    @Delete
    abstract suspend fun deleteBookmarkLocation(bookmarkLocation: BookmarksLocations)

    /**
     * Adds or removes a bookmark location and updates markers.
     * @return `true` if added, `false` if removed.
     */
    suspend fun updateBookmarkLocation(bookmarkLocation: Place): Boolean {
        val exists = findBookmarkLocation(bookmarkLocation.name)

        if (exists) {
            deleteBookmarkLocation(toEntity(bookmarkLocation))
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, false)
        } else {
            addBookmarkLocation(toEntity(bookmarkLocation))
            NearbyController.updateMarkerLabelListBookmark(bookmarkLocation, true)
        }

        return !exists
    }

    /**
     * Fetches all bookmark locations as `Place` objects.
     */
    suspend fun getAllBookmarksLocationsPlace(): List<Place> {
        return getAllBookmarksLocations().map { fromEntity(it) }
    }

    fun toEntity(place: Place): BookmarksLocations {
        return BookmarksLocations(
            locationName = place.name,
            locationLanguage = place.language,
            locationDescription = place.longDescription,
            locationCategory = place.category,
            locationLat = place.location.latitude,
            locationLong = place.location.longitude,
            locationLabelText = place.label?.text ?: "",
            locationLabelIcon = place.label?.icon,
            locationImageUrl = place.pic,
            locationWikipediaLink = place.siteLinks.wikipediaLink.toString(),
            locationWikidataLink = place.siteLinks.wikidataLink.toString(),
            locationCommonsLink = place.siteLinks.commonsLink.toString(),
            locationPic = place.pic,
            locationExists = place.exists
        )
    }

    private fun fromEntity(entity: BookmarksLocations): Place {
        val location = fr.free.nrw.commons.location.LatLng(
            entity.locationLat,
            entity.locationLong,
            1F
        )

        val builder = fr.free.nrw.commons.nearby.Sitelinks.Builder().apply {
            setWikipediaLink(entity.locationWikipediaLink)
            setWikidataLink(entity.locationWikidataLink)
            setCommonsLink(entity.locationCommonsLink)
        }

        return Place(
            entity.locationLanguage,
            entity.locationName,
            fr.free.nrw.commons.nearby.Label.fromText(entity.locationLabelText),
            entity.locationDescription,
            location,
            entity.locationCategory,
            builder.build(),
            entity.locationPic,
            entity.locationExists
        )
    }
}
