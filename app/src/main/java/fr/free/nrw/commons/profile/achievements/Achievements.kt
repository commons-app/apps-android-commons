package fr.free.nrw.commons.profile.achievements

/**
 * Represents Achievements class and stores all the parameters
 */
class Achievements {
    /**
     * The count of unique images used by the wiki.
     * @return The count of unique images used.
     * @param uniqueUsedImages The count to set for unique images used.
     */
    var uniqueUsedImages = 0
    private var articlesUsingImages = 0

    /**
     * The count of thanks received.
     * @return The count of thanks received.
     * @param thanksReceived The count to set for thanks received.
     */
    var thanksReceived = 0

    /**
     * The count of featured images.
     * @return The count of featured images.
     * @param featuredImages The count to set for featured images.
     */
    var featuredImages = 0

    /**
     * The count of quality images.
     * @return The count of quality images.
     * @param qualityImages The count to set for quality images.
     */
    var qualityImages = 0

    /**
     * The count of images uploaded.
     * @return The count of images uploaded.
     * @param imagesUploaded The count to set for images uploaded.
     */
    var imagesUploaded = 0
    private var revertCount = 0

    constructor() {}

    /**
     * constructor for achievements class to set its data members
     * @param uniqueUsedImages
     * @param articlesUsingImages
     * @param thanksReceived
     * @param featuredImages
     * @param imagesUploaded
     * @param revertCount
     */
    constructor(
        uniqueUsedImages: Int,
        articlesUsingImages: Int,
        thanksReceived: Int,
        featuredImages: Int,
        qualityImages: Int,
        imagesUploaded: Int,
        revertCount: Int,
    ) {
        this.uniqueUsedImages = uniqueUsedImages
        this.articlesUsingImages = articlesUsingImages
        this.thanksReceived = thanksReceived
        this.featuredImages = featuredImages
        this.qualityImages = qualityImages
        this.imagesUploaded = imagesUploaded
        this.revertCount = revertCount
    }

    /**
     * used to calculate the percentages of images that haven't been reverted
     * @return
     */
    val notRevertPercentage: Int
        get() =
            try {
                (imagesUploaded - revertCount) * 100 / imagesUploaded
            } catch (divideByZero: ArithmeticException) {
                100
            }

    companion object {
        /**
         * Get Achievements object from FeedbackResponse
         *
         * @param response
         * @return
         */
        @JvmStatic
        fun from(response: FeedbackResponse): Achievements =
            Achievements(
                response.uniqueUsedImages,
                response.articlesUsingImages,
                response.thanksReceived,
                response.featuredImages.featuredPicturesOnWikimediaCommons,
                response.featuredImages.qualityImages,
                0,
                response.deletedUploads,
            )
    }
}
