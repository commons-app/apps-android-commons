package fr.free.nrw.commons.category

import androidx.room.*
import io.reactivex.Completable
import io.reactivex.Single
import java.util.*

@Dao
abstract class CategoryRoomDao {

    @Query("SELECT * FROM categories ORDER BY last_used DESC LIMIT :limit")
    abstract fun getAll(limit: Int): Single<List<CategoryRoomEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(category: CategoryRoomEntity): Single<Long>

    @Update
    abstract fun update(category: CategoryRoomEntity): Completable

    @Query("SELECT * FROM categories WHERE name = :name")
    abstract fun findEntity(name: String): Single<List<CategoryRoomEntity>>

    @Query("SELECT EXISTS (SELECT 1 FROM categories WHERE name = :name)")
    abstract fun findCategory(name: String): Single<Boolean>

    @Query("DELETE FROM categories")
    abstract fun deleteAll(): Completable

    fun recentCategories(limit: Int): Single<List<CategoryItem>> {
        return getAll(limit).map { entities ->
            entities.map { fromEntity(it) }
        }
    }

    fun save(category: Category): Completable {
        return findEntity(category.name ?: "").flatMapCompletable { entities ->
            if (entities.isNotEmpty()) {
                update(toEntity(category, entities[0].id))
            } else {
                insert(toEntity(category)).ignoreElement()
            }
        }
    }

    fun toEntity(
        category: Category,
        id: Long = 0
    ): CategoryRoomEntity = CategoryRoomEntity(
        id = id,
        name = category.name ?: "",
        description = category.description,
        thumbnail = category.thumbnail,
        lastUsed = category.lastUsed ?: Date(),
        timesUsed = category.timesUsed
    )

    private fun fromEntity(
        entity: CategoryRoomEntity
    ): CategoryItem = CategoryItem(
        name = entity.name,
        description = entity.description,
        thumbnail = entity.thumbnail,
        isSelected = false,
    )
}
