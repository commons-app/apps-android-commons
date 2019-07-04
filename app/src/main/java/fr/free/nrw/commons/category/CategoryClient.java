package fr.free.nrw.commons.category;


import org.wikipedia.dataclient.mwapi.MwQueryPage;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import io.reactivex.Observable;
import timber.log.Timber;

/**
 * Category Client to handle custom calls to Commons MediaWiki APIs
 */
@Singleton
public class CategoryClient {

    private final CategoryInterface CategoryInterface;

    @Inject
    public CategoryClient(CategoryInterface CategoryInterface) {
        this.CategoryInterface = CategoryInterface;
    }

    /**
     * Searches for categories with the specified name.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    public Observable<String> searchCategories(String filter, int itemLimit) {
        return CategoryInterface.searchCategories(filter, itemLimit)
                .flatMap(mwQueryResponse -> {
                    List<MwQueryPage> pages = mwQueryResponse.query().pages();
                    if(pages != null)
                        return Observable.fromIterable(pages);
                    else
                        Timber.d("No categories returned.");
                        return Observable.empty();
                })
                .map(MwQueryPage::title)
                .doOnEach(s->Timber.d("Category returned: %s", s))
                .map(cat->cat.replace("Category:", ""));
    }
}