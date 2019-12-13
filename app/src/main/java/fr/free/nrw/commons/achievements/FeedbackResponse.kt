package fr.free.nrw.commons.achievements

data class FeedbackResponse(val uniqueUsedImages: Int,
                            val articlesUsingImages: Int,
                            val deletedUploads: Int,
                            val featuredImages: FeaturedImages,
                            val thanksReceived: Int,
                            val user: String)