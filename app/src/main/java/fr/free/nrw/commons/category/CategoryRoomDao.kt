package fr.free.nrw.commons.category

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import io.reactivex.Completable
import io.reactivex.Single

@Dao
interface CategoryRoomDao {

    @Query("SELECT * FROM categories ORDER BY last_used DESC LIMIT :limit")
    fun getAll(limit: Int): Single<List<CategoryRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(category: CategoryRoomEntity)

    @Query("SELECT * FROM categories WHERE name = :name")
    fun findEntity(name: String): CategoryRoomEntity?

    @Query("SELECT EXISTS (SELECT 1 FROM categories WHERE name = :name)")
    fun findCategory(name: String): Boolean

    @Query("DELETE FROM categories")
    fun deleteAll(): Completable
}
