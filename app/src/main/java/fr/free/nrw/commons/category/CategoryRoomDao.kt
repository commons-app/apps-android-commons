package fr.free.nrw.commons.category

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*

@Dao
abstract class CategoryRoomDao {

    @Query("SELECT * FROM categories ORDER BY last_used DESC LIMIT :limit")
    protected abstract fun getAllInternal(limit: Int): Single<List<CategoryRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(category: CategoryRoomEntity): Single<Long>

    @Update
    protected abstract fun updateInternal(category: CategoryRoomEntity): Completable

    @Query("SELECT * FROM categories WHERE name = :name")
    protected abstract fun findEntity(name: String): Single<List<CategoryRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM categories WHERE name = :name)")
    abstract fun findCategory(name: String): Single<Boolean>

    @Query("DELETE FROM categories")
    abstract fun deleteAll(): Completable

    fun recentCategories(limit: Int): Single<List<CategoryItem>> {
        return getAllInternal(limit).map { entities ->
            entities.map { CategoryItem(it.name, it.description, it.thumbnail, false) }
        }
    }

    fun save(category: Category): Completable {
        return findEntity(category.name ?: "").flatMapCompletable { entities ->
            if (entities.isNotEmpty()) {
                updateInternal(
                    CategoryRoomEntity(
                        id = entities[0].id,
                        name = category.name ?: "",
                        description = category.description,
                        thumbnail = category.thumbnail,
                        lastUsed = category.lastUsed ?: Date(),
                        timesUsed = category.timesUsed
                    )
                )
            } else {
                insertInternal(
                    CategoryRoomEntity(
                        name = category.name ?: "",
                        description = category.description,
                        thumbnail = category.thumbnail,
                        lastUsed = category.lastUsed ?: Date(),
                        timesUsed = category.timesUsed
                    )
                ).ignoreElement()
            }
        }
    }
}
