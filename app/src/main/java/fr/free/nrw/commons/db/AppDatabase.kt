package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(entities = [Contribution::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun contributionDao(): ContributionDao
}
