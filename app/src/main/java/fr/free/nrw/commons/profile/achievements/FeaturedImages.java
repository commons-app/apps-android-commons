package fr.free.nrw.commons.profile.achievements;

import com.google.gson.annotations.SerializedName;

public class FeaturedImages {

    @SerializedName("Quality_images")
    private final int qualityImages;

    @SerializedName("Featured_pictures_on_Wikimedia_Commons")
    private final int featuredPicturesOnWikimediaCommons;

    public FeaturedImages(int qualityImages, int featuredPicturesOnWikimediaCommons) {
        this.qualityImages = qualityImages;
        this.featuredPicturesOnWikimediaCommons = featuredPicturesOnWikimediaCommons;
    }

    public int getQualityImages() {
        return qualityImages;
    }

    public int getFeaturedPicturesOnWikimediaCommons() {
        return featuredPicturesOnWikimediaCommons;
    }
}
