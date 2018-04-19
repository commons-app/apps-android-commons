package fr.free.nrw.commons.featured;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class FeaturedImageController {

    private static final String FEATURED_IMAGES_CATEGORY = "Category:Featured_pictures_on_Wikimedia_Commons";

    private MediaWikiApi mediaWikiApi;

    @Inject
    public FeaturedImageController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    public List<FeaturedImage> getFeaturedImages() {
        List<Media> categoryImages = mediaWikiApi.getCategoryImages(FEATURED_IMAGES_CATEGORY);
        List<FeaturedImage> featuredImages = new ArrayList<>();
        for (Media media : categoryImages) {
            featuredImages.add(new FeaturedImage(media));
        }
        return featuredImages;
    }
}