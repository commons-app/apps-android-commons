package fr.free.nrw.commons.profile.achievements

/**
 * Represents Achievements data class and stores all the parameters.
 * Immutable version with default values for optional properties.
 */
data class Achievements(
    val uniqueUsedImages: Int = 0,
    val articlesUsingImages: Int = 0,
    val thanksReceived: Int = 0,
    val featuredImages: Int = 0,
    val qualityImages: Int = 0,
    val imagesUploaded: Int = 0,
    val revertCount: Int = 0
) {
    /**
     * Used to calculate the percentages of images that haven't been reverted.
     * Returns 100 if imagesUploaded is 0 to avoid division by zero.
     */
    val notRevertPercentage: Int
        get() = if (imagesUploaded > 0) {
            (imagesUploaded - revertCount) * 100 / imagesUploaded
        } else {
            100
        }

    companion object {
        /**
         * Get Achievements object from FeedbackResponse.
         *
         * @param response The feedback response to convert.
         * @return An Achievements object with values from the response.
         */
        @JvmStatic
        fun from(response: FeedbackResponse): Achievements = Achievements(
            uniqueUsedImages = response.uniqueUsedImages,
            articlesUsingImages = response.articlesUsingImages,
            thanksReceived = response.thanksReceived,
            featuredImages = response.featuredImages.featuredPicturesOnWikimediaCommons,
            qualityImages = response.featuredImages.qualityImages,
            imagesUploaded = 0,  // Assuming imagesUploaded should be 0
            revertCount = response.deletedUploads
        )
    }
}