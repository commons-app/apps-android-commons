package fr.free.nrw.commons.customselector.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Calendar

/**
 * UploadedStatusDao for Custom Selector.
 */
@Dao
abstract class UploadedStatusDao {
    /**
     * Insert into uploaded status.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(uploadedStatus: UploadedStatus)

    /**
     * Update uploaded status entry.
     */
    @Update
    abstract fun update(uploadedStatus: UploadedStatus)

    /**
     * Delete uploaded status entry.
     */
    @Delete
    abstract fun delete(uploadedStatus: UploadedStatus)

    /**
     * Query uploaded status with image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract fun getFromImageSHA1(imageSHA1: String): UploadedStatus?

    /**
     * Query uploaded status with modified image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) ")
    abstract fun getFromModifiedImageSHA1(modifiedImageSHA1: String): UploadedStatus?

    /**
     * Asynchronous insert into uploaded status table.
     */
    suspend fun insertUploaded(uploadedStatus: UploadedStatus) {
        uploadedStatus.lastUpdated = Calendar.getInstance().time
        insert(uploadedStatus)
    }

    /**
     * Check whether the imageSHA1 is present in database
     */
    @Query("SELECT COUNT() FROM uploaded_table WHERE imageSHA1 = (:imageSHA1) AND imageResult = (:imageResult) ")
    abstract fun findByImageSHA1(
        imageSHA1: String,
        imageResult: Boolean,
    ): Int

    /**
     * Check whether the modifiedImageSHA1 is present in database
     */
    @Query(
        "SELECT COUNT() FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) AND modifiedImageResult = (:modifiedImageResult) ",
    )
    abstract fun findByModifiedImageSHA1(
        modifiedImageSHA1: String,
        modifiedImageResult: Boolean,
    ): Int

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getUploadedFromImageSHA1(imageSHA1: String): UploadedStatus? = getFromImageSHA1(imageSHA1)
}
