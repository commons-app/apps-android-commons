package fr.free.nrw.commons.recentlanguages

import androidx.room.*
import io.reactivex.Single

@Dao
abstract class RecentLanguagesRoomDao {

    @Query("SELECT * FROM recent_languages ORDER BY rowid DESC")
    protected abstract fun getAllInternal(): Single<List<RecentLanguageRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(language: RecentLanguageRoomEntity)

    @Query("DELETE FROM recent_languages WHERE language_code = :languageCode")
    abstract fun deleteRecentLanguage(languageCode: String)

    @Query("SELECT EXISTS (SELECT 1 FROM recent_languages WHERE language_code = :languageCode)")
    abstract fun findRecentLanguage(languageCode: String): Boolean

    fun getRecentLanguages(): List<Language> {
        return getAllInternal().blockingGet().map { Language(it.languageName, it.languageCode) }
    }

    fun addRecentLanguage(language: Language) {
        insert(RecentLanguageRoomEntity(language.languageName, language.languageCode))
    }
}
