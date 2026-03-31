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
    abstract fun insert(category: CategoryRoomEntity)

    @Update
    abstract fun update(category: CategoryRoomEntity)

    @Query("SELECT * FROM categories WHERE name = :name")
    abstract fun findEntity(name: String): CategoryRoomEntity?

    @Query("SELECT EXISTS (SELECT 1 FROM categories WHERE name = :name)")
    abstract fun findCategory(name: String): Boolean

    @Query("DELETE FROM categories")
    abstract fun deleteAll(): Completable

    fun recentCategories(limit: Int): Single<List<CategoryItem>> {
        return getAllInternal(limit).map { entities ->
            entities.map { CategoryItem(it.name, it.description, it.thumbnail, false) }
        }
    }

    fun save(category: Category) {
        val existing = findEntity(category.name ?: "")
        if (existing != null) {
            update(
                CategoryRoomEntity(
                    id = existing.id,
                    name = category.name ?: "",
                    description = category.description,
                    thumbnail = category.thumbnail,
                    lastUsed = category.lastUsed ?: Date(),
                    timesUsed = category.timesUsed
                )
            )
        } else {
            insert(
                CategoryRoomEntity(
                    name = category.name ?: "",
                    description = category.description,
                    thumbnail = category.thumbnail,
                    lastUsed = category.lastUsed ?: Date(),
                    timesUsed = category.timesUsed
                )
            )
        }
    }
}
