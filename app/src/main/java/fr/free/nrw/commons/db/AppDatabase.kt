package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoriesDao
import fr.free.nrw.commons.bookmarks.category.BookmarkCategoryRoomEntity
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsRoomDao
import fr.free.nrw.commons.bookmarks.items.BookmarkItemsRoomEntity
import fr.free.nrw.commons.bookmarks.locations.BookmarkLocationsDao
import fr.free.nrw.commons.bookmarks.locations.BookmarksLocations
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPictureRoomEntity
import fr.free.nrw.commons.bookmarks.pictures.BookmarkPicturesRoomDao
import fr.free.nrw.commons.category.CategoryRoomDao
import fr.free.nrw.commons.category.CategoryRoomEntity
import fr.free.nrw.commons.contributions.ContributionRoomEntity
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.UploadedStatusRoomEntity
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.customselector.database.NotForUploadStatusRoomEntity
import fr.free.nrw.commons.customselector.database.NotForUploadStatusDao
import fr.free.nrw.commons.explore.recentsearches.RecentSearchRoomEntity
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesRoomDao
import fr.free.nrw.commons.nearby.PlaceRoomEntity
import fr.free.nrw.commons.nearby.PlaceDao
import fr.free.nrw.commons.recentlanguages.RecentLanguageRoomEntity
import fr.free.nrw.commons.recentlanguages.RecentLanguagesRoomDao
import fr.free.nrw.commons.review.ReviewRoomEntity
import fr.free.nrw.commons.review.ReviewDao
import fr.free.nrw.commons.upload.depicts.DepictsRoomEntity
import fr.free.nrw.commons.upload.depicts.DepictsDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(
    entities = [ContributionRoomEntity::class, DepictsRoomEntity::class,
        UploadedStatusRoomEntity::class, NotForUploadStatusRoomEntity::class,
        ReviewRoomEntity::class, PlaceRoomEntity::class,
        BookmarkCategoryRoomEntity::class, BookmarksLocations::class,
        BookmarkPictureRoomEntity::class, BookmarkItemsRoomEntity::class,
        CategoryRoomEntity::class, RecentLanguageRoomEntity::class, RecentSearchRoomEntity::class
    ],
    version = 22,
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
    abstract fun categoryRoomDao(): CategoryRoomDao
    abstract fun bookmarkPicturesRoomDao(): BookmarkPicturesRoomDao
    abstract fun bookmarkItemsRoomDao(): BookmarkItemsRoomDao
    abstract fun recentLanguagesRoomDao(): RecentLanguagesRoomDao
    abstract fun recentSearchesRoomDao(): RecentSearchesRoomDao
}
