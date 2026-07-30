package fr.free.nrw.commons.recentlanguages

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Single

@Dao
interface RecentLanguagesRoomDao {

    @Query("SELECT * FROM recent_languages ORDER BY rowid DESC")
    fun getAll(): Single<List<RecentLanguageRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(language: RecentLanguageRoomEntity)

    @Query("DELETE FROM recent_languages WHERE language_code = :languageCode")
    fun deleteRecentLanguage(languageCode: String)

    @Query("SELECT EXISTS (SELECT 1 FROM recent_languages WHERE language_code = :languageCode)")
    fun findRecentLanguage(languageCode: String): Boolean
}
