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
    val revertCount: Int = 0,
) {
    /**
     * Percentage of uploaded images that were not deleted.
     * Returns 100 when there are no uploads. Floors at 0 when deleted uploads exceed the
     * upload count, which can happen when those two values come from different sources.
     */
    val notRevertPercentage: Int
        get() =
            if (imagesUploaded > 0) {
                ((imagesUploaded - revertCount) * 100 / imagesUploaded).coerceIn(0, 100)
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
        fun from(response: FeedbackResponse): Achievements =
            Achievements(
                uniqueUsedImages = response.uniqueUsedImages,
                articlesUsingImages = response.articlesUsingImages,
                thanksReceived = response.thanksReceived,
                featuredImages = response.featuredImages.featuredPicturesOnWikimediaCommons,
                qualityImages = response.featuredImages.qualityImages,
                imagesUploaded = 0, // Assuming imagesUploaded should be 0
                revertCount = response.deletedUploads,
            )
    }
}
