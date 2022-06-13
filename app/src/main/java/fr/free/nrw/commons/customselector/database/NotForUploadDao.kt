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
}


