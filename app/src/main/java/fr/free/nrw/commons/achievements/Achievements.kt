package fr.free.nrw.commons.achievements

/**
 * Represents Achievements class and stores all the parameters
 */
class Achievements {
    /**
     * getter function to get count of unique images used by wiki
     * @return
     */
    /**
     * setter function to set count of uniques images used by wiki
     * @param uniqueUsedImages
     */
    var uniqueUsedImages = 0
    private var articlesUsingImages = 0
    /**
     * getter function to get count of thanks received
     * @return
     */
    /**
     * setter function to set count of thanks received
     * @param thanksReceived
     */
    var thanksReceived = 0
    /**
     * getter function to get count of featured images
     * @return
     */
    /**
     * setter function to set count of featured images
     * @param featuredImages
     */
    var featuredImages = 0
    /**
     * getter function to get count of images uploaded
     * @return
     */
    /**
     * setter function to count of images uploaded
     * @param imagesUploaded
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
    constructor(uniqueUsedImages: Int,
                articlesUsingImages: Int,
                thanksReceived: Int,
                featuredImages: Int,
                imagesUploaded: Int,
                revertCount: Int) {
        this.uniqueUsedImages = uniqueUsedImages
        this.articlesUsingImages = articlesUsingImages
        this.thanksReceived = thanksReceived
        this.featuredImages = featuredImages
        this.imagesUploaded = imagesUploaded
        this.revertCount = revertCount
    }

    /**
     * used to calculate the percentages of images that haven't been reverted
     * @return
     */
    val notRevertPercentage: Int
        get() = try {
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
        fun from(response: FeedbackResponse): Achievements {
            return Achievements(response.uniqueUsedImages,
                    response.articlesUsingImages,
                    response.thanksReceived,
                    response.featuredImages.qualityImages
                            + response.featuredImages.featuredPicturesOnWikimediaCommons, 0,
                    response.deletedUploads)
        }
    }
}