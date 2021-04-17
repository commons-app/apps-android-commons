package fr.free.nrw.commons.upload.depicts

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = arrayOf(Depicts::class), version = 1, exportSchema = false)
@TypeConverters(depictsRoomDataBaseConverter::class)
abstract class DepictsRoomDataBase : RoomDatabase() {

    abstract fun DepictsDao(): DepictsDao

    companion object {
        // Singleton prevents multiple instances of database opening at the
        // same time.
        @Volatile
        private var INSTANCE: DepictsRoomDataBase? = null

        fun getDatabase(context: Context): DepictsRoomDataBase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DepictsRoomDataBase::class.java,
                    "depicts_database"
                ).build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

}