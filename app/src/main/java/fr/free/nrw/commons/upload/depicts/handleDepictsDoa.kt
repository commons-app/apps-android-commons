package fr.free.nrw.commons.upload.depicts

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

/**
 *  Handle all methods of DepictsDoa
 */
public class handleDepictsDoa(application: Application) {
    lateinit var allDepicts: List<Depicts>;
    lateinit var depictsRoomDataBase: DepictsRoomDataBase;
    lateinit var depictsDoa: DepictsDao;
    lateinit var listOfDelete: List<Depicts>;


    init {
        depictsRoomDataBase = DepictsRoomDataBase.getDatabase(application);
        depictsDoa = depictsRoomDataBase.DepictsDao();
        runBlocking {
            launch(Dispatchers.IO) {
                allDepicts = depictsDoa.getAllDepict();
            }
        }
    }

    /**
     *  insert Depicts  in DepictsRoomDataBase
     */
    fun insert(depictes: Depicts) {
        runBlocking {
            val job = launch(Dispatchers.IO) {
                depictsDoa.insert(depictes);
            }
        }
    }

    /**
     *  get all Depicts item which need to delete
     */
    fun getItemTodelete(number: Int): List<Depicts> {
        runBlocking {
            launch(Dispatchers.IO) {
                listOfDelete = depictsDoa.getItemToDelete(number);
            }
        }
        return listOfDelete;
    }

    /**
     *  delete Depicts  in DepictsRoomDataBase
     */
    fun deleteDepicts(depictes: Depicts) {
        runBlocking {
            launch(Dispatchers.IO) {
                depictsDoa.delete(depictes);
            }
        }
    }

}

