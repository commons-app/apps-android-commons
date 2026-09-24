package fr.free.nrw.commons.upload.depicts

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.util.Date

/**
 *  Dao class for DepictsRoomDataBase
 */
@Dao
abstract class DepictsDao {
    /** The maximum number of depicts allowed in the database. */
    private val maxItemsAllowed = 10

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(depicts: DepictsRoomEntity)

    fun insertDepict(depict: Depicts) =
        CoroutineScope(Dispatchers.IO).launch {
            insertInternal(toEntity(depict))
        }

    @Query("Select * From depicts_table order by lastUsed DESC")
    protected abstract fun getAllDepictsInternal(): List<DepictsRoomEntity>

    fun getAllDepicts(): List<Depicts> =
        getAllDepictsInternal().map { fromEntity(it) }

    @Query("Select * From depicts_table order by lastUsed DESC LIMIT :n OFFSET 10")
    protected abstract fun getDepictsForDeletionInternal(n: Int): List<DepictsRoomEntity>

    fun getDepictsForDeletion(n: Int): List<Depicts> =
        getDepictsForDeletionInternal(n).map { fromEntity(it) }

    /**
     * Gets all Depicts objects from the database, ordered by lastUsed in descending order.
     *
     * @return Deferred list of Depicts objects.
     */
    fun depictsList(): Deferred<List<Depicts>> =
        CoroutineScope(Dispatchers.IO).async {
            getAllDepicts()
        }

    /**
     * Gets a list of Depicts objects that need to be deleted from the database.
     *
     * @param n The number of depicts to delete.
     * @return A list of Depicts objects to delete.
     */
    fun depictsForDeletion(n: Int): Deferred<List<Depicts>> =
        CoroutineScope(Dispatchers.IO).async {
            getDepictsForDeletion(n)
        }

    @Delete
    protected abstract fun deleteInternal(depicts: DepictsRoomEntity)

    fun deleteDepicts(depicts: Depicts) =
        CoroutineScope(Dispatchers.IO).launch {
            deleteInternal(toEntity(depicts))
        }

    private fun toEntity(depicts: Depicts): DepictsRoomEntity =
        DepictsRoomEntity(
            item = depicts.item,
            lastUsed = depicts.lastUsed
        )

    private fun fromEntity(entity: DepictsRoomEntity): Depicts =
        Depicts(
            item = entity.item,
            lastUsed = entity.lastUsed
        )

    /**
     * Saves a list of DepictedItems in the DepictsRoomDataBase.
     */
    fun savingDepictsInRoomDataBase(listDepictedItem: List<DepictedItem>) {
        CoroutineScope(Dispatchers.IO).launch {
            for (depictsItem in listDepictedItem) {
                depictsItem.isSelected = false
                insertDepict(Depicts(depictsItem, Date()))
            }

            // Deletes old Depicts objects from the database if
            // the number of depicts exceeds the maximum allowed.
            deleteOldDepictions(depictsList().await().size)
        }
    }

    private suspend fun deleteOldDepictions(depictsListSize: Int) {
        if (depictsListSize > maxItemsAllowed) {
            val depictsForDeletion = depictsForDeletion(depictsListSize).await()

            for (depicts in depictsForDeletion) {
                deleteDepicts(depicts)
            }
        }
    }
}
