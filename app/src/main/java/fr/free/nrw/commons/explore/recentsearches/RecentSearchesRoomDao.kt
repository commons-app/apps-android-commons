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
    protected abstract fun insertInternal(recentSearch: RecentSearchRoomEntity): Single<Long>

    @Update
    protected abstract fun updateInternal(recentSearch: RecentSearchRoomEntity): Completable

    @Query("SELECT * FROM recent_searches WHERE name = :query")
    protected abstract fun findEntity(query: String): Single<List<RecentSearchRoomEntity>>

    @Query("DELETE FROM recent_searches")
    abstract fun deleteTable(): Completable

    @Query("DELETE FROM recent_searches WHERE name = :name")
    abstract fun deleteByName(name: String): Completable

    fun recentSearches(limit: Int): Single<List<String>> {
        return getAllInternal(limit).map { entities ->
            entities.map { it.query }
        }
    }

    fun find(query: String): Single<RecentSearch?> {
        return findEntity(query).map { entities ->
            entities.firstOrNull()?.let { RecentSearch(it.query, it.lastSearched) }
        }
    }

    fun delete(recentSearch: RecentSearch): Completable {
        return deleteByName(recentSearch.query)
    }

    fun deleteAll(): Completable {
        return deleteTable()
    }

    fun save(recentSearch: RecentSearch): Completable {
        return findEntity(recentSearch.query).flatMapCompletable { entities ->
            if (entities.isNotEmpty()) {
                updateInternal(
                    RecentSearchRoomEntity(
                        id = entities[0].id,
                        query = recentSearch.query,
                        lastSearched = recentSearch.lastSearched
                    )
                )
            } else {
                insertInternal(
                    RecentSearchRoomEntity(
                        query = recentSearch.query,
                        lastSearched = recentSearch.lastSearched
                    )
                ).ignoreElement()
            }
        }
    }
}
