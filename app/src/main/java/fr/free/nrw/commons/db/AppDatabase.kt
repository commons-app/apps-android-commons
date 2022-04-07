package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.contributions.models.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.customselector.database.UploadedStatus
import fr.free.nrw.commons.customselector.database.UploadedStatusDao
import fr.free.nrw.commons.upload.depicts.Depicts
import fr.free.nrw.commons.upload.depicts.DepictsDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(entities = [Contribution::class, Depicts::class, UploadedStatus::class], version = 10, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun contributionDao(): ContributionDao
    abstract fun DepictsDao(): DepictsDao;
    abstract fun UploadedStatusDao(): UploadedStatusDao;
}
