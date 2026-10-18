package fr.free.nrw.commons.explore.recentsearches

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface RecentSearchesRoomDao {

    @Query("SELECT * FROM recent_searches ORDER BY last_used DESC LIMIT :limit")
    fun getAll(limit: Int): Single<List<RecentSearchRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(recentSearch: RecentSearchRoomEntity)

    @Query("SELECT * FROM recent_searches WHERE name = :query")
    fun findEntity(query: String): RecentSearchRoomEntity?

    @Query("DELETE FROM recent_searches")
    fun deleteTable(): Completable
}
