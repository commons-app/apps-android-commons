package fr.free.nrw.commons.category;


import androidx.annotation.NonNull;

import org.wikipedia.dataclient.mwapi.MwQueryPage;
import org.wikipedia.dataclient.mwapi.MwQueryResponse;
import org.wikipedia.dataclient.mwapi.MwQueryResult;

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
     * The method takes categoryName as input and returns a List of Subcategories
     * It uses the generator query API to get the subcategories in a category, 500 at a time.
     *
     * @param categoryName Category name as defined on commons
     * @return Observable emitting the categories returned. If our search yielded "Category:Test", "Test" is emitted.
     */
    public Observable<String> getSubCategoryList(String categoryName) {
        return responseToCategoryName(CategoryInterface.getSubCategoryList(categoryName));
    }

    /**
     * The method takes categoryName as input and returns a List of parent categories
     * It uses the generator query API to get the parent categories of a category, 500 at a time.
     *
     * @param categoryName Category name as defined on commons
     * @return
     */
    @NonNull
    public Observable<String> getParentCategoryList(String categoryName) {
        return responseToCategoryName(CategoryInterface.getParentCategoryList(categoryName));
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
                    MwQueryResult query;
                    List<MwQueryPage> pages;
                    if ((query = mwQueryResponse.query()) == null ||
                            (pages = query.pages()) == null) {
                        Timber.d("No categories returned.");
                        return Observable.empty();
                    } else
                        return Observable.fromIterable(pages);
                })
                .map(MwQueryPage::title)
                .doOnEach(s -> Timber.d("Category returned: %s", s))
                .map(cat -> cat.replace("Category:", ""));
    }
}