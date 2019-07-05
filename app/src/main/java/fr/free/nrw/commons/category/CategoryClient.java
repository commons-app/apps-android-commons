package fr.free.nrw.commons.category;


import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;

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
     * Searches for categories containing the specified string.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @param offset    Starts returning items from the nth result. If offset is 9, the response starts with the 9th item of the search result
     * @return
     */
    public Observable<String> searchCategories(String filter, int itemLimit, int offset) {
        return responseToCategoryName(CategoryInterface.searchCategories(filter, itemLimit, offset));

    }

    /**
     * Searches for categories containing the specified string.
     *
     * @param filter    The string to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    public Observable<String> searchCategories(String filter, int itemLimit) {
        return searchCategories(filter, itemLimit, 0);

    }

    /**
     * Searches for categories starting with the specified string.
     *
     * @param prefix    The prefix to be searched
     * @param itemLimit How many results are returned
     * @param offset    Starts returning items from the nth result. If offset is 9, the response starts with the 9th item of the search result
     * @return
     */
    public Observable<String> searchCategoriesForPrefix(String prefix, int itemLimit, int offset) {
        return responseToCategoryName(CategoryInterface.searchCategoriesForPrefix(prefix, itemLimit, offset));
    }

    /**
     * Searches for categories starting with the specified string.
     *
     * @param prefix    The prefix to be searched
     * @param itemLimit How many results are returned
     * @return
     */
    public Observable<String> searchCategoriesForPrefix(String prefix, int itemLimit) {
        return searchCategoriesForPrefix(prefix, itemLimit, 0);
    }


    /**
     * Internal function to reduce code reuse. Extracts the categories returned from MwQueryResponse.
     *
     * @param responseObservable The query response observable
     * @return Observable emitting the categories returned. If our search yielded "Category:Test", "Test" is emitted.
     */
    private Observable<String> responseToCategoryName(Observable<MwQueryResponse> responseObservable) {
        return responseObservable
                .flatMap(mwQueryResponse -> {
                    List<MwQueryPage> pages = mwQueryResponse.query().pages();
                    if (pages != null)
                        return Observable.fromIterable(pages);
                    else
                        Timber.d("No categories returned.");
                    return Observable.empty();
                })
                .map(MwQueryPage::title)
                .doOnEach(s -> Timber.d("Category returned: %s", s))
                .map(cat -> cat.replace("Category:", ""));
    }
}