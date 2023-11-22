package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.*
import fr.free.nrw.commons.review.ReviewDao
import fr.free.nrw.commons.review.ReviewEntity
import fr.free.nrw.commons.upload.depicts.Depicts
import fr.free.nrw.commons.upload.depicts.DepictsDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(entities = [Contribution::class, Depicts::class, UploadedStatus::class, NotForUploadStatus::class, ReviewEntity::class], version = 16, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contributionDao(): ContributionDao
    abstract fun DepictsDao(): DepictsDao;
    abstract fun UploadedStatusDao(): UploadedStatusDao;
    abstract fun NotForUploadStatusDao(): NotForUploadStatusDao
    abstract fun ReviewDao(): ReviewDao
}
