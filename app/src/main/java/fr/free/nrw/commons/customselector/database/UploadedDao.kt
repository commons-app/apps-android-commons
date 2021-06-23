package fr.free.nrw.commons.customselector.database

import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

/**
 * UploadedStatusDao for Custom Selector.
 */
@Dao
abstract class UploadedStatusDao {

    /**
     * Insert into uploaded status.
     */
    @Insert( onConflict = OnConflictStrategy.REPLACE )
    abstract suspend fun insert(uploadedStatus: UploadedStatus)

    /**
     * Update uploaded status entry.
     */
    @Update
    abstract suspend fun update(uploadedStatus: UploadedStatus)

    /**
     * Delete uploaded status entry.
     */
    @Delete
    abstract suspend fun delete(uploadedStatus: UploadedStatus)

    /**
     * Get All entries from the uploaded status table.
     */
    @Query("SELECT * FROM uploaded_table")
    abstract suspend fun getAll() : List<UploadedStatus>

    /**
     * Query uploaded status with image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun getFromImageSHA1(imageSHA1 : String) : UploadedStatus

    /**
     * Query uploaded status with modified image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE modifiedImageSHA1 = (:imageSHA1) ")
    abstract suspend fun getFromModifiedImageSHA1(imageSHA1 : String) : UploadedStatus

    /**
     * Asynchronous insert into uploaded status table.
     */
    fun insertUploaded(uploadedStatus: UploadedStatus) {
        runBlocking {
            launch(Dispatchers.IO) {
                uploadedStatus.lastUpdated = Calendar.getInstance().time as Date?
                insert(uploadedStatus)
            }
        }
    }

    /**
     * Asynchronous delete from uploaded status table.
     */
    fun deleteUploaded(uploadedStatus: UploadedStatus) {
        runBlocking {
            launch(Dispatchers.IO) {
                delete(uploadedStatus)
            }
        }
    }

    /**
     * Asynchronous update entry in uploaded status table.
     */
    fun updateUploaded(uploadedStatus: UploadedStatus) {
        runBlocking {
            launch(Dispatchers.IO) {
                uploadedStatus.lastUpdated = Calendar.getInstance().time as Date?
                update(uploadedStatus)
            }
        }
    }

    /**
     * Asynchronous image sha1 query.
     */
    fun getUploadedFromImageSHA1(imageSHA1: String) : UploadedStatus? {
        var queryResult : UploadedStatus? = null
        runBlocking {
            launch(Dispatchers.IO) {
               queryResult = getFromImageSHA1(imageSHA1)
            }
        }
        return queryResult
    }

    /**
     * Asynchronous modified image sha1 query.
     */
    fun getUploadedFromModifiedImageSHA1(imageSHA1: String) : UploadedStatus? {
        var queryResult : UploadedStatus? = null
        runBlocking {
            launch(Dispatchers.IO) {
                queryResult = getFromModifiedImageSHA1(imageSHA1)
            }
        }
        return queryResult
    }

}