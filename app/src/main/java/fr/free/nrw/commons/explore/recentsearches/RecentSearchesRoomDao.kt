package fr.free.nrw.commons.explore.recentsearches

import androidx.room.*
import fr.free.nrw.commons.explore.models.RecentSearch
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*

@Dao
abstract class RecentSearchesRoomDao {

    @Query("SELECT * FROM recent_searches ORDER BY last_used DESC LIMIT :limit")
    abstract fun getAll(limit: Int): Single<List<RecentSearchRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(recentSearch: RecentSearchRoomEntity): Single<Long>

    @Update
    abstract fun update(recentSearch: RecentSearchRoomEntity): Completable

    @Query("SELECT * FROM recent_searches WHERE name = :query")
    abstract fun findEntity(query: String): Single<List<RecentSearchRoomEntity>>

    @Query("DELETE FROM recent_searches")
    abstract fun deleteTable(): Completable

    @Query("DELETE FROM recent_searches WHERE name = :name")
    abstract fun deleteByName(name: String): Completable

    fun recentSearches(limit: Int): Single<List<String>> {
        return getAll(limit).map { entities ->
            entities.map { it.query }
        }
    }

    fun find(query: String): Single<RecentSearch?> {
        return findEntity(query).map { entities ->
            entities.firstOrNull()?.let { fromEntity(it) }
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
                update(toEntity(recentSearch, entities[0].id))
            } else {
                insert(toEntity(recentSearch)).ignoreElement()
            }
        }
    }

    private fun toEntity(
        recentSearch: RecentSearch,
        id: Long = 0
    ): RecentSearchRoomEntity = RecentSearchRoomEntity(
        id = id,
        query = recentSearch.query,
        lastSearched = recentSearch.lastSearched
    )

    private fun fromEntity(
        entity: RecentSearchRoomEntity
    ): RecentSearch = RecentSearch(
        query = entity.query,
        lastSearched = entity.lastSearched
    )
}
