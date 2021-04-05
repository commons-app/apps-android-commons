package fr.free.nrw.commons.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import fr.free.nrw.commons.contributions.Contribution
import fr.free.nrw.commons.contributions.ContributionDao

/**
 * The database for accessing the respective DAOs
 *
 */
@Database(entities = [Contribution::class,UploadedImages::class], version = 8, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

  abstract fun contributionDao(): ContributionDao

  abstract val uploadedImagesDao :UploadedImagesDao

  companion object{

    @Volatile
    private var INSTANCE : AppDatabase? = null

    fun getInstance(context:Context): AppDatabase{
      synchronized(this){
        var instance = INSTANCE
        if (instance == null){
          instance = Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "app_database"
          ).build()
          INSTANCE = instance
        }
        return instance
      }
    }
  }
}
