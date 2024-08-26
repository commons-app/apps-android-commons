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
    abstract suspend fun insert(depictedItem: Depicts)
    
    @Query("Select * From depicts_table order by lastUsed DESC")
    abstract suspend fun getAllDepicts(): List<Depicts>

    @Query("Select * From depicts_table order by lastUsed DESC LIMIT :n OFFSET 10")
    abstract suspend fun getDepictsForDeletion(n: Int): List<Depicts>

    @Delete
    abstract suspend fun delete(depicts: Depicts)

    /**
     * Gets all Depicts objects from the database, ordered by lastUsed in descending order.
     *
     * @return A list of Depicts objects.
     */
    fun depictsList(): Deferred<List<Depicts>> = CoroutineScope(Dispatchers.IO).async {
        getAllDepicts()
    }

    /**
     * Inserts a Depicts object into the database.
     *
     * @param depictedItem The Depicts object to insert.
     */
    private fun insertDepict(depictedItem: Depicts) = CoroutineScope(Dispatchers.IO).launch { 
        insert(depictedItem)
    }

    /**
     * Gets a list of Depicts objects that need to be deleted from the database.
     *
     * @param n The number of depicts to delete.
     * @return A list of Depicts objects to delete.
     */
    private suspend fun depictsForDeletion(n: Int): Deferred<List<Depicts>> = CoroutineScope(Dispatchers.IO).async {
        getDepictsForDeletion(n)
    }

    /**
     * Deletes a Depicts object from the database.
     *
     * @param depicts The Depicts object to delete.
     */
    private suspend fun deleteDepicts(depicts: Depicts) = CoroutineScope(Dispatchers.IO).launch {
        delete(depicts)
    }

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
        if(depictsListSize > maxItemsAllowed) {
            val depictsForDeletion = depictsForDeletion(depictsListSize).await()

            for(depicts in depictsForDeletion) {
                deleteDepicts(depicts)
            }
        }
    }
}

