package fr.free.nrw.commons.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao
import fr.free.nrw.commons.depictions.RecentDepictions
import fr.free.nrw.commons.depictions.RecentDepictionsDao

/**
 * The database for accessing the respective DAOs
 *
 */

@Database(entities = arrayOf(
          Contribution::class,
          RecentDepictions::class),
          version = 1,
          exportSchema = false)

@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
  abstract fun contributionDao(): ContributionDao
  abstract fun recentDepictionsDao(): RecentDepictionsDao
}
