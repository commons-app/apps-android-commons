package fr.free.nrw.commons.customselector.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
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
     * Query uploaded status with image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun getFromImageSHA1(imageSHA1: String): UploadedStatus?

    /**
     * Query uploaded status with modified image sha1.
     */
    @Query("SELECT * FROM uploaded_table WHERE modifiedImageSHA1 = (:modifiedImageSHA1) ")
    abstract suspend fun getFromModifiedImageSHA1(modifiedImageSHA1: String): UploadedStatus?

    /**
     * Records an upload or lookup result. A negative lookup that finishes after the upload
     * must not overwrite the confirmed publication.
     */
    @Transaction
    open suspend fun insertUploaded(uploadedStatus: UploadedStatus) {
        val foundUpload = uploadedStatus.imageResult || uploadedStatus.modifiedImageResult
        if (
            !foundUpload &&
            (
                countUploadedBySourceSHA1(uploadedStatus.imageSHA1) > 0 ||
                    findByModifiedImageSHA1(uploadedStatus.modifiedImageSHA1, true) > 0
            )
        ) {
            return
        }
        uploadedStatus.lastUpdated = Calendar.getInstance().time
        insert(uploadedStatus)
    }

    /**
     * Counts confirmed uploads of the source image, including edited versions.
     */
    @Query(
        "SELECT COUNT(*) FROM uploaded_table WHERE imageSHA1 = :imageSHA1 " +
            "AND (imageResult = 1 OR modifiedImageResult = 1)",
    )
    abstract suspend fun countUploadedBySourceSHA1(imageSHA1: String): Int

    /**
     * Check whether the modifiedImageSHA1 is present in database
     */
    @Query(
        "SELECT COUNT() FROM uploaded_table WHERE modifiedImage" +
            "SHA1 = (:modifiedImageSHA1) AND modifiedImageResult = " +
            "(:modifiedImageResult) ",
    )
    abstract suspend fun findByModifiedImageSHA1(
        modifiedImageSHA1: String,
        modifiedImageResult: Boolean,
    ): Int

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getUploadedFromImageSHA1(imageSHA1: String): UploadedStatus? = getFromImageSHA1(imageSHA1)
}
