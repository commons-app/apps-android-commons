package fr.free.nrw.commons.review

import androidx.room.*

/**
 * Dao interface for reviewed images database
 */
@Dao
abstract class ReviewDao {

    /**
     * Inserts reviewed/skipped image identifier into the database internally
     *
     * @param reviewRoomEntity
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract fun insertInternal(reviewRoomEntity: ReviewRoomEntity)

    /**
     * Public method to insert using domain model
     */
    fun insert(reviewImage: ReviewImage) {
        insertInternal(toEntity(reviewImage))
    }

    /**
     * Checks if the image has already been reviewed/skipped by the user
     * Returns true if the identifier exists in the reviewed images table
     *
     * @param imageId
     * @return
     */
    @Query("SELECT EXISTS (SELECT * from `reviewed-images` where imageId = (:imageId))")
    abstract fun isReviewedAlready(imageId: String): Boolean

    private fun toEntity(reviewImage: ReviewImage): ReviewRoomEntity =
        ReviewRoomEntity(imageId = reviewImage.imageId)
}
