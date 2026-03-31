package fr.free.nrw.commons.explore.recentsearches

import androidx.room.*
import fr.free.nrw.commons.explore.models.RecentSearch
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*

@Dao
abstract class RecentSearchesRoomDao {

    @Query("SELECT * FROM recent_searches ORDER BY last_used DESC LIMIT :limit")
    protected abstract fun getAllInternal(limit: Int): Single<List<RecentSearchRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(recentSearch: RecentSearchRoomEntity)

    @Update
    abstract fun update(recentSearch: RecentSearchRoomEntity)

    @Query("SELECT * FROM recent_searches WHERE name = :query")
    abstract fun findEntity(query: String): RecentSearchRoomEntity?

    @Query("DELETE FROM recent_searches")
    abstract fun deleteTable(): Completable

    @Query("DELETE FROM recent_searches WHERE name = :name")
    abstract fun deleteByName(name: String)

    fun recentSearches(limit: Int): List<String> {
        return getAllInternal(limit).blockingGet().map { it.query }
    }

    fun find(query: String): RecentSearch? {
        return findEntity(query)?.let { RecentSearch(it.query, it.lastSearched) }
    }

    fun delete(recentSearch: RecentSearch) {
        deleteByName(recentSearch.query)
    }

    fun deleteAll() {
        deleteTable().blockingAwait()
    }

    fun save(recentSearch: RecentSearch) {
        val existing = findEntity(recentSearch.query)
        if (existing != null) {
            update(
                RecentSearchRoomEntity(
                    id = existing.id,
                    query = recentSearch.query,
                    lastSearched = recentSearch.lastSearched
                )
            )
        } else {
            insert(
                RecentSearchRoomEntity(
                    query = recentSearch.query,
                    lastSearched = recentSearch.lastSearched
                )
            )
        }
    }
}
