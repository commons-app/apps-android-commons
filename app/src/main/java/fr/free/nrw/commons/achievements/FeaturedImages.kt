package fr.free.nrw.commons.achievements

import com.google.gson.annotations.SerializedName

data class FeaturedImages(
    @SerializedName("Quality_images") val qualityImages: Int,
    @SerializedName("Featured_pictures_on_Wikimedia_Commons") val featuredPicturesOnWikimediaCommons: Int
)
