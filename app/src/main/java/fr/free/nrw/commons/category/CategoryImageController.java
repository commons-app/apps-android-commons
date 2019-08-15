package fr.free.nrw.commons.category;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.Single;

@Singleton
public class CategoryImageController {

    private OkHttpJsonApiClient okHttpJsonApiClient;

    @Inject
    public CategoryImageController(OkHttpJsonApiClient okHttpJsonApiClient) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }

    /**
     * Takes a category name as input and calls the API to get a list of images for that category
     * @param categoryName
     * @return
     */
    public Single<List<Media>> getCategoryImages(String categoryName) {
        return okHttpJsonApiClient.getMediaList("category", categoryName);
    }
}
