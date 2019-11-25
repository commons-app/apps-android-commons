package fr.free.nrw.commons.category;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.GpsCategoryModel;
import fr.free.nrw.commons.utils.StringSortingUtils;
import io.reactivex.Observable;
import timber.log.Timber;

/**
 * The model class for categories in upload
 */
public class CategoriesModel{
    private static final int SEARCH_CATS_LIMIT = 25;

    private final CategoryClient categoryClient;
    private final CategoryDao categoryDao;
    private final JsonKvStore directKvStore;

    private HashMap<String, ArrayList<String>> categoriesCache;
    private List<CategoryItem> selectedCategories;

    @Inject GpsCategoryModel gpsCategoryModel;
    @Inject
    public CategoriesModel(CategoryClient categoryClient,
                           CategoryDao categoryDao,
                           @Named("default_preferences") JsonKvStore directKvStore) {
        this.categoryClient = categoryClient;
        this.categoryDao = categoryDao;
        this.directKvStore = directKvStore;
        this.categoriesCache = new HashMap<>();
        this.selectedCategories = new ArrayList<>();
    }

    /**
     * Sorts CategoryItem by similarity
     * @param filter
     * @return
     */
    public Comparator<CategoryItem> sortBySimilarity(final String filter) {
        Comparator<String> stringSimilarityComparator = StringSortingUtils.sortBySimilarity(filter);
        return (firstItem, secondItem) -> stringSimilarityComparator
                .compare(firstItem.getName(), secondItem.getName());
    }

    /**
     * Returns if the item contains an year
     * @param item
     * @return
     */
    public boolean containsYear(String item) {
        //Check for current and previous year to exclude these categories from removal
        Calendar now = Calendar.getInstance();
        int year = now.get(Calendar.YEAR);
        String yearInString = String.valueOf(year);

        int prevYear = year - 1;
        String prevYearInString = String.valueOf(prevYear);
        Timber.d("Previous year: %s", prevYearInString);

        //Check if item contains a 4-digit word anywhere within the string (.* is wildcard)
        //And that item does not equal the current year or previous year
        //And if it is an irrelevant category such as Media_needing_categories_as_of_16_June_2017(Issue #750)
        //Check if the year in the form of XX(X)0s is relevant, i.e. in the 2000s or 2010s as stated in Issue #1029
        return ((item.matches(".*(19|20)\\d{2}.*") && !item.contains(yearInString) && !item.contains(prevYearInString))
                || item.matches("(.*)needing(.*)") || item.matches("(.*)taken on(.*)")
                || (item.matches(".*0s.*") && !item.matches(".*(200|201)0s.*")));
    }

    /**
     * Updates category count in category dao
     * @param item
     */
    public void updateCategoryCount(CategoryItem item) {
        Category category = categoryDao.find(item.getName());

        // Newly used category...
        if (category == null) {
            category = new Category(null, item.getName(), new Date(), 0);
        }

        category.incTimesUsed();
        categoryDao.save(category);
    }

    boolean cacheContainsKey(String term) {
        return categoriesCache.containsKey(term);
    }
    //endregion

    /**
     * Regional category search
     * @param term
     * @param imageTitleList
     * @return
     */
    public Observable<CategoryItem> searchAll(String term, List<String> imageTitleList) {
        //If query text is empty, show him category based on gps and title and recent searches
        if (TextUtils.isEmpty(term)) {
            Observable<CategoryItem> categoryItemObservable = gpsCategories()
                    .concatWith(titleCategories(imageTitleList));
            if (hasDirectCategories()) {
                categoryItemObservable.concatWith(directCategories().concatWith(recentCategories()));
            }
            return categoryItemObservable;
        }

        //if user types in something that is in cache, return cached category
        if (cacheContainsKey(term)) {
            return Observable.fromIterable(getCachedCategories(term))
                    .map(name -> new CategoryItem(name, false));
        }

        //otherwise, search API for matching categories
        return categoryClient
                .searchCategoriesForPrefix(term, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }


    /**
     * Returns cached categories
     * @param term
     * @return
     */
    private ArrayList<String> getCachedCategories(String term) {
        return categoriesCache.get(term);
    }

    /**
     * Returns if we have a category in DirectKV Store
     * @return
     */
    private boolean hasDirectCategories() {
        return !directKvStore.getString("Category", "").equals("");
    }

    /**
     * Returns categories in DirectKVStore
     * @return
     */
    private Observable<CategoryItem> directCategories() {
        String directCategory = directKvStore.getString("Category", "");
        List<String> categoryList = new ArrayList<>();
        Timber.d("Direct category found: " + directCategory);

        if (!directCategory.equals("")) {
            categoryList.add(directCategory);
            Timber.d("DirectCat does not equal emptyString. Direct Cat list has " + categoryList);
        }
        return Observable.fromIterable(categoryList).map(name -> new CategoryItem(name, false));
    }

    /**
     * Returns GPS categories
     * @return
     */
    Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(gpsCategoryModel.getCategoryList())
                .map(name -> new CategoryItem(name, false));
    }

    /**
     * Returns title based categories
     * @param titleList
     * @return
     */
    private Observable<CategoryItem> titleCategories(List<String> titleList) {
        return Observable.fromIterable(titleList)
                .concatMap(this::getTitleCategories);
    }

    /**
     * Return category for single title
     * @param title
     * @return
     */
    private Observable<CategoryItem> getTitleCategories(String title) {
        return categoryClient.searchCategories(title, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    /**
     * Returns recent categories
     * @return
     */
    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(categoryDao.recentCategories(SEARCH_CATS_LIMIT))
                .map(s -> new CategoryItem(s, false));
    }

    /**
     * Handles category item selection
     * @param item
     */
    public void onCategoryItemClicked(CategoryItem item) {
        if (item.isSelected()) {
            selectCategory(item);
            updateCategoryCount(item);
        } else {
            unselectCategory(item);
        }
    }

    /**
     * Select's category
     * @param item
     */
    public void selectCategory(CategoryItem item) {
        selectedCategories.add(item);
    }

    /**
     * Unselect Category
     * @param item
     */
    public void unselectCategory(CategoryItem item) {
        selectedCategories.remove(item);
    }


    /**
     * Get Selected Categories
     * @return
     */
    public List<CategoryItem> getSelectedCategories() {
        return selectedCategories;
    }

    /**
     * Get Categories String List
     * @return
     */
    public List<String> getCategoryStringList() {
        List<String> output = new ArrayList<>();
        for (CategoryItem item : selectedCategories) {
            output.add(item.getName());
        }
        return output;
    }

    /**
     * Cleanup the existing in memory cache's
     */
    public void cleanUp() {
        this.categoriesCache.clear();
        this.selectedCategories.clear();
    }
}
