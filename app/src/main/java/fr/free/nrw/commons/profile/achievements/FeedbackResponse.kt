package fr.free.nrw.commons.profile.achievements

/**
 * Represent the Feedback Response of the user
 */
data class FeedbackResponse(val uniqueUsedImages: Int,
                            val articlesUsingImages: Int,
                            val deletedUploads: Int,
                            val featuredImages: FeaturedImages,
                            val thanksReceived: Int,
                            val user: String)