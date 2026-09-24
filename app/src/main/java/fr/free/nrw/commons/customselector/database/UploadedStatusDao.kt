package fr.free.nrw.commons.customselector.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import java.util.Calendar
import fr.free.nrw.commons.customselector.database.UploadedStatusRoomEntity

/**
 * UploadedStatusDao for Custom Selector.
 */
@Dao
abstract class UploadedStatusDao {
    /**
     * Insert into uploaded status.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(uploadedStatus: UploadedStatusRoomEntity)

    suspend fun insert(uploadedStatus: UploadedStatus) {
        insertInternal(toEntity(uploadedStatus))
    }

    /**
     * Update uploaded status entry.
     */
    @Update
    protected abstract fun updateInternal(uploadedStatus: UploadedStatusRoomEntity)

    suspend fun update(uploadedStatus: UploadedStatus) {
        updateInternal(toEntity(uploadedStatus))
    }

    /**
     * Delete uploaded status entry.
     */
    @Delete
    protected abstract fun deleteInternal(uploadedStatus: UploadedStatusRoomEntity)

    suspend fun delete(uploadedStatus: UploadedStatus) {
        deleteInternal(toEntity(uploadedStatus))
    }

    /**
     * Query uploaded status with image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE imageSHA1 = (:imageSHA1) ")
    protected abstract fun getFromImageSHA1Internal(imageSHA1: String): UploadedStatusRoomEntity?

    suspend fun getFromImageSHA1(imageSHA1: String): UploadedStatus? {
        val entity = getFromImageSHA1Internal(imageSHA1)
        return if (entity != null) fromEntity(entity) else null
    }

    /**
     * Query uploaded status with modified image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) ")
    protected abstract fun getFromModifiedImageSHA1Internal(modifiedImageSHA1: String): UploadedStatusRoomEntity?

    suspend fun getFromModifiedImageSHA1(modifiedImageSHA1: String): UploadedStatus? {
        val entity = getFromModifiedImageSHA1Internal(modifiedImageSHA1)
        return if (entity != null) fromEntity(entity) else null
    }

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
    abstract suspend fun findByImageSHA1(
        imageSHA1: String,
        imageResult: Boolean,
    ): Int

    /**
     * Check whether the modifiedImageSHA1 is present in database
     */
    @Query(
        "SELECT COUNT() FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) AND modifiedImageResult = (:modifiedImageResult) ",
    )
    abstract suspend fun findByModifiedImageSHA1(
        modifiedImageSHA1: String,
        modifiedImageResult: Boolean,
    ): Int

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getUploadedFromImageSHA1(imageSHA1: String): UploadedStatus? = getFromImageSHA1(imageSHA1)

    private fun toEntity(uploadedStatus: UploadedStatus): UploadedStatusRoomEntity =
        UploadedStatusRoomEntity(
            imageSHA1 = uploadedStatus.imageSHA1,
            modifiedImageSHA1 = uploadedStatus.modifiedImageSHA1,
            imageResult = uploadedStatus.imageResult,
            modifiedImageResult = uploadedStatus.modifiedImageResult,
            lastUpdated = uploadedStatus.lastUpdated
        )

    private fun fromEntity(entity: UploadedStatusRoomEntity): UploadedStatus =
        UploadedStatus(
            imageSHA1 = entity.imageSHA1,
            modifiedImageSHA1 = entity.modifiedImageSHA1,
            imageResult = entity.imageResult,
            modifiedImageResult = entity.modifiedImageResult,
            lastUpdated = entity.lastUpdated
        )
}
