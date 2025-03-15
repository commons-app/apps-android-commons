package fr.free.nrw.commons.profile.model


import com.google.gson.annotations.SerializedName

data class AchievementResponse(
    @SerializedName("articlesUsingImages")
    val articlesUsingImages: Int,
    @SerializedName("database")
    val database: String,
    @SerializedName("deletedUploads")
    val deletedUploads: Int,
    @SerializedName("featuredImages")
    val featuredImages: FeaturedImages,
    @SerializedName("imagesEditedBySomeoneElse")
    val imagesEditedBySomeoneElse: Int,
    @SerializedName("labs")
    val labs: Boolean,
    @SerializedName("status")
    val status: String,
    @SerializedName("thanksReceived")
    val thanksReceived: Int,
    @SerializedName("uniqueUsedImages")
    val uniqueUsedImages: Int,
    @SerializedName("user")
    val user: String
)

data class FeaturedImages(
    @SerializedName("Featured_pictures_on_Wikimedia_Commons")
    val featuredPicturesOnWikimediaCommons: Int,
    @SerializedName("Quality_images")
    val qualityImages: Int
)