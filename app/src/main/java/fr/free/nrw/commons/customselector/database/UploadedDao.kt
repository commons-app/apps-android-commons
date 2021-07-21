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
    suspend fun insertUploaded(uploadedStatus: UploadedStatus) {
        uploadedStatus.lastUpdated = Calendar.getInstance().time as Date?
        insert(uploadedStatus)
    }

    /**
     * Asynchronous delete from uploaded status table.
     */
    suspend fun deleteUploaded(uploadedStatus: UploadedStatus) {
        delete(uploadedStatus)
    }

    /**
     * Asynchronous update entry in uploaded status table.
     */
    suspend fun updateUploaded(uploadedStatus: UploadedStatus) {
        update(uploadedStatus)
    }

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getUploadedFromImageSHA1(imageSHA1: String):UploadedStatus {
        return getFromImageSHA1(imageSHA1)
    }

    /**
     * Asynchronous modified image sha1 query.
     */
    suspend fun getUploadedFromModifiedImageSHA1(modifiedImageSHA1: String):UploadedStatus {
        return getFromModifiedImageSHA1(modifiedImageSHA1)
    }

}