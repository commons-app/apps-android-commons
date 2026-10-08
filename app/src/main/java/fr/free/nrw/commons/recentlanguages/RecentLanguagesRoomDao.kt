package fr.free.nrw.commons.recentlanguages

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single

@Dao
abstract class RecentLanguagesRoomDao {

    @Query("SELECT * FROM recent_languages ORDER BY rowid DESC")
    abstract fun getAll(): Single<List<RecentLanguageRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(language: RecentLanguageRoomEntity): Completable

    @Query("DELETE FROM recent_languages WHERE language_code = :languageCode")
    abstract fun deleteRecentLanguage(languageCode: String): Completable

    @Query("SELECT EXISTS (SELECT 1 FROM recent_languages WHERE language_code = :languageCode)")
    abstract fun findRecentLanguage(languageCode: String): Single<Boolean>

    fun getRecentLanguages(): Single<List<Language>> {
        return getAll().map { entities ->
            entities.map { fromEntity(it) }
        }
    }

    fun addRecentLanguage(language: Language): Completable {
        return insert(toEntity(language))
    }

    private fun toEntity(language: Language): RecentLanguageRoomEntity {
        return RecentLanguageRoomEntity(language.languageName, language.languageCode)
    }

    private fun fromEntity(entity: RecentLanguageRoomEntity): Language {
        return Language(entity.languageName, entity.languageCode)
    }
}
