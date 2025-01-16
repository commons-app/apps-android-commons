package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.category.BookmarksCategoryModal
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarksLocations
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.NotForUploadStatus
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.nearby.Place
import fr.free.nrw.commons.nearby.PlaceDao
import fr.free.nrw.commons.review.ReviewDao
import fr.free.nrw.commons.review.ReviewEntity
import fr.free.nrw.commons.upload.depicts.Depicts
import fr.free.nrw.commons.upload.depicts.DepictsDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(
    entities = [Contribution::class, Depicts::class, UploadedStatus::class, NotForUploadStatus::class, ReviewEntity::class, Place::class, BookmarksCategoryModal::class, BookmarksLocations::class],
    version = 19,
    exportSchema = false,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contributionDao(): ContributionDao

    abstract fun PlaceDao(): PlaceDao

    abstract fun DepictsDao(): DepictsDao

    abstract fun UploadedStatusDao(): UploadedStatusDao

    abstract fun NotForUploadStatusDao(): NotForUploadStatusDao

    abstract fun ReviewDao(): ReviewDao

    abstract fun bookmarkCategoriesDao(): BookmarkCategoriesDao

    abstract fun bookmarkLocationsDao(): BookmarkLocationsDao
}
