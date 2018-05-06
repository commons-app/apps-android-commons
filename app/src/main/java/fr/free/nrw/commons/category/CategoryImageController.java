package fr.free.nrw.commons.category;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.MediaWikiApi;

@Singleton
public class CategoryImageController {

    private MediaWikiApi mediaWikiApi;

    @Inject
    public CategoryImageController(MediaWikiApi mediaWikiApi) {
        this.mediaWikiApi = mediaWikiApi;
    }

    /**
     * Takes a category name as input and calls the API to get a list of images for that category
     * @param categoryName
     * @return
     */
    public List<Media> getCategoryImages(String categoryName) {
        return mediaWikiApi.getCategoryImages(categoryName);
    }
}