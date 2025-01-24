package fr.free.nrw.commons.review

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Dao interface for reviewed images database
 */
@Dao
interface ReviewDao {

    /**
     * Inserts reviewed/skipped image identifier into the database
     *
     * @param reviewEntity
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(reviewEntity: ReviewEntity)

    /**
     * Checks if the image has already been reviewed/skipped by the user
     * Returns true if the identifier exists in the reviewed images table
     *
     * @param imageId
     * @return
     */
    @Query("SELECT EXISTS (SELECT * from `reviewed-images` where imageId = (:imageId))")
    fun isReviewedAlready(imageId: String): Boolean
}
