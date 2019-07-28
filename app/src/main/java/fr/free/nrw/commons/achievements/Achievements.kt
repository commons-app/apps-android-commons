package fr.free.nrw.commons.achievements

data class Achievements(
    val uniqueUsedImages: Int,
    private val articlesUsingImages: Int,
    val thanksReceived: Int,
    private val imagesEditedBySomeoneElse: Int,
    val featuredImages: Int,
    var imagesUploaded: Int,
    private val revertCount: Int
) {

    val notRevertPercentage: Int
        get() {
            return try {
                (imagesUploaded - revertCount) * 100 / imagesUploaded
            } catch (divideByZero: ArithmeticException) {
                100
            }
        }

    companion object {

        @JvmStatic
        fun from(response: FeedbackResponse) = Achievements(
            response.uniqueUsedImages,
            response.articlesUsingImages,
            response.thanksReceived,
            response.imagesEditedBySomeoneElse,
            response.featuredImages.qualityImages + response.featuredImages.featuredPicturesOnWikimediaCommons,
            0,
            response.deletedUploads
        )
    }
}
