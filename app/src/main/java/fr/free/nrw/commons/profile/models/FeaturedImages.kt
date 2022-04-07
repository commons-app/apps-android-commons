package fr.free.nrw.commons.profile.models

import com.google.gson.annotations.SerializedName

/**
 * Represents Featured Images on WikiMedia Commons platform
 * Used by Achievements and FeedbackResponse (objects) of the user
 */
class FeaturedImages(
    @field:SerializedName("Quality_images") val qualityImages: Int,
    @field:SerializedName("Featured_pictures_on_Wikimedia_Commons") val featuredPicturesOnWikimediaCommons: Int
)