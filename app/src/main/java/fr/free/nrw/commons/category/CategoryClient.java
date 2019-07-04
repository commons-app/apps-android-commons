package fr.free.nrw.commons.category;


import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Single;
import retrofit2.http.Query;

/**
 * Category Client to handle custom calls to Commons CategoryWiki APIs
 */
@Singleton
public class CategoryClient {

    private final CategoryInterface CategoryInterface;

    @Inject
    public CategoryClient(CategoryInterface CategoryInterface) {
        this.CategoryInterface = CategoryInterface;
    }

    /**
     *
     */
    public Single<Boolean> searchCategories(String filterValue, int searchCatsLimit) {
        return CategoryInterface.searchCategories(filterValue, searchCatsLimit)
                .map(mwQueryResponse -> mwQueryResponse
                        .query().firstPage().pageId() > 0)
                .singleOrError();
    }
}