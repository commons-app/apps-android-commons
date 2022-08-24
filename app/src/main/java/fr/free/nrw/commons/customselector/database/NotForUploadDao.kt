package fr.free.nrw.commons.customselector.database

import androidx.room.*


/**
 * Dao class for Not For Upload
 */
@Dao
abstract class NotForUploadStatusDao {

    /**
     * Insert into Not For Upload status.
     */
    @Insert( onConflict = OnConflictStrategy.REPLACE )
    abstract suspend fun insert(notForUploadStatus: NotForUploadStatus)

    /**
     * Delete Not For Upload status entry.
     */
    @Delete
    abstract suspend fun delete(notForUploadStatus: NotForUploadStatus)

    /**
     * Query Not For Upload status with image sha1.
     */
    @Query("SELECT * FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun getFromImageSHA1(imageSHA1 : String) : NotForUploadStatus?

    /**
     * Asynchronous image sha1 query.
     */
    suspend fun getNotForUploadFromImageSHA1(imageSHA1: String):NotForUploadStatus? {
        return getFromImageSHA1(imageSHA1)
    }

    /**
     * Deletion Not For Upload status with image sha1.
     */
    @Query("DELETE FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun deleteWithImageSHA1(imageSHA1 : String)

    /**
     * Asynchronous image sha1 deletion.
     */
    suspend fun deleteNotForUploadWithImageSHA1(imageSHA1: String) {
        return deleteWithImageSHA1(imageSHA1)
    }

    /**
     * Check whether the imageSHA1 is present in database
     */
    @Query("SELECT COUNT() FROM images_not_for_upload_table WHERE imageSHA1 = (:imageSHA1) ")
    abstract suspend fun find(imageSHA1 : String): Int
}


