package fr.free.nrw.commons.upload.depicts

import androidx.room.*
import fr.free.nrw.commons.upload.models.depictions.DepictedItem
import fr.free.nrw.commons.upload.models.depictions.Depicts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 *  Dao class for DepictsRoomDataBase
 */
@Dao
abstract class DepictsDao {

    /**
     *  insert Depicts in DepictsRoomDataBase
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(depictedItem: Depicts)

    /**
     * get all Depicts from roomdatabase
     */
    @Query("Select * From depicts_table order by lastUsed DESC")
    abstract suspend fun getAllDepict(): List<Depicts>

    /**
     *  get all Depicts which need to delete  from roomdatabase
     */
    @Query("Select * From depicts_table order by lastUsed DESC LIMIT :n OFFSET 10")
    abstract suspend fun getItemToDelete(n: Int): List<Depicts>

    /**
     *  Delete Depicts from roomdatabase
     */
    @Delete
    abstract suspend fun delete(depicts: Depicts)

    lateinit var allDepict: List<Depicts>
    lateinit var listOfDelete: List<Depicts>

    /**
     * get all depicts from DepictsRoomDatabase
     */
    fun depictsList(): List<Depicts> {
        runBlocking {
            launch(Dispatchers.IO) {
                allDepict = getAllDepict()
            }
        }
        return allDepict
    }

    /**
     *  insert Depicts  in DepictsRoomDataBase
     */
    fun insertDepict(depictes: Depicts) {
        runBlocking {
            launch(Dispatchers.IO) {
                insert(depictes)
            }
        }
    }

    /**
     *  get all Depicts item which need to delete
     */
    fun getItemTodelete(number: Int): List<Depicts> {
        runBlocking {
            launch(Dispatchers.IO) {
                listOfDelete = getItemToDelete(number)
            }
        }
        return listOfDelete
    }

    /**
     *  delete Depicts  in DepictsRoomDataBase
     */
    fun deleteDepicts(depictes: Depicts) {
        runBlocking {
            launch(Dispatchers.IO) {
                delete(depictes)
            }
        }
    }

    /**
     *  save Depicts in DepictsRoomDataBase
     */
    fun savingDepictsInRoomDataBase(listDepictedItem: List<DepictedItem>) {
        var numberofItemInRoomDataBase: Int
        val maxNumberOfItemSaveInRoom = 10

        for (depictsItem in listDepictedItem) {
            depictsItem.isSelected = false
            insertDepict(Depicts(depictsItem, Date()))
        }

        numberofItemInRoomDataBase = depictsList().size
        // delete the depictItem from depictsroomdataBase when number of element in depictsroomdataBase is greater than 10
        if (numberofItemInRoomDataBase > maxNumberOfItemSaveInRoom) {

            val listOfDepictsToDelete: List<Depicts> =
                getItemTodelete(numberofItemInRoomDataBase)
            for (i in listOfDepictsToDelete) {
                deleteDepicts(i)
            }
        }
    }
}