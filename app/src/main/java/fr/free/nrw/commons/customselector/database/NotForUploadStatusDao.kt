package fr.free.nrw.commons.customselector.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import fr.free.nrw.commons.customselector.database.NotForUploadStatusRoomEntity

/**
 * Dao class for Not For Upload
 */
@Dao
abstract class NotForUploadStatusDao {
    /**
     * Insert into Not For Upload status.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract fun insertInternal(notForUploadStatus: NotForUploadStatusRoomEntity)

    suspend fun insert(notForUploadStatus: NotForUploadStatus) {
        insertInternal(toEntity(notForUploadStatus))
    }

    /**
     * Delete Not For Upload status entry.
     */
    @Delete
    protected abstract fun deleteInternal(notForUploadStatus: NotForUploadStatusRoomEntity)

    suspend fun delete(notForUploadStatus: NotForUploadStatus) {
        deleteInternal(toEntity(notForUploadStatus))
    }

    /**
     * Query Not For Upload status with image sha1.
     */
    @Query("SELECT * FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    protected abstract fun getFromImageSHA1Internal(imageSHA1: String): NotForUploadStatusRoomEntity?

    suspend fun getFromImageSHA1(imageSHA1: String): NotForUploadStatus? {
        val entity = getFromImageSHA1Internal(imageSHA1)
        return if (entity != null) fromEntity(entity) else null
    }

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getNotForUploadFromImageSHA1(imageSHA1: String): NotForUploadStatus? = getFromImageSHA1(imageSHA1)

    /**
     * Deletion Not For Upload status with image sha1.
     */
    @Query("DELETE FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun deleteWithImageSHA1(imageSHA1: String)

    /**
     * Asynchronous image sha1 deletion.
     */
    suspend fun deleteNotForUploadWithImageSHA1(imageSHA1: String) = deleteWithImageSHA1(imageSHA1)

    /**
     * Check whether the imageSHA1 is present in database
     */
    @Query("SELECT COUNT() FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun find(imageSHA1: String): Int

    private fun toEntity(notForUploadStatus: NotForUploadStatus): NotForUploadStatusRoomEntity =
        NotForUploadStatusRoomEntity(
            imageSHA1 = notForUploadStatus.imageSHA1
        )

    private fun fromEntity(entity: NotForUploadStatusRoomEntity): NotForUploadStatus =
        NotForUploadStatus(
            imageSHA1 = entity.imageSHA1
        )
}
