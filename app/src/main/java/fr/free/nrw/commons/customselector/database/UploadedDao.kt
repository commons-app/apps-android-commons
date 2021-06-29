package fr.free.nrw.commons.customselector.database

import androidx.room.*
import kotlinx.coroutines.runBlocking
import java.util.*
import kotlinx.coroutines.*

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
    @Query("SELECT * FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) ")
    abstract suspend fun getFromModifiedImageSHA1(modifiedImageSHA1 : String) : UploadedStatus

    /**
     * Asynchronous insert into uploaded status table.
     */
    fun insertUploaded(uploadedStatus: UploadedStatus) = runBlocking {
        async {
                uploadedStatus.lastUpdated = Calendar.getInstance().time as Date?
                insert(uploadedStatus)
            }.await()
    }

    /**
     * Asynchronous delete from uploaded status table.
     */
    fun deleteUploaded(uploadedStatus: UploadedStatus) = runBlocking {
        async { delete(uploadedStatus) }
    }

    /**
     * Asynchronous update entry in uploaded status table.
     */
    fun updateUploaded(uploadedStatus: UploadedStatus) = runBlocking {
        async { update(uploadedStatus) }
    }

    /**
     * Asynchronous image sha1 query.
     */
    fun getUploadedFromImageSHA1(imageSHA1: String) = runBlocking<UploadedStatus?> {
        async { getFromImageSHA1(imageSHA1) }.await()
    }

    /**
     * Asynchronous modified image sha1 query.
     */
    fun getUploadedFromModifiedImageSHA1(modifiedImageSHA1: String) = runBlocking<UploadedStatus?> {
        async { getFromModifiedImageSHA1(modifiedImageSHA1) }.await()
    }

}