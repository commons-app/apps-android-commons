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
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.GpsCategoryModel;
import fr.free.nrw.commons.utils.StringSortingUtils;
import io.reactivex.Observable;
import timber.log.Timber;

public class CategoriesModel implements CategoryClickedListener {
    private static final int SEARCH_CATS_LIMIT = 25;

    private final MediaWikiApi mwApi;
    private final CategoryDao categoryDao;
    private final JsonKvStore directKvStore;

    private HashMap<String, ArrayList<String>> categoriesCache;
    private List<CategoryItem> selectedCategories;

    @Inject GpsCategoryModel gpsCategoryModel;
    @Inject
    public CategoriesModel(MediaWikiApi mwApi,
                           CategoryDao categoryDao,
                           @Named("direct_nearby_upload_prefs") JsonKvStore directKvStore) {
        this.mwApi = mwApi;
        this.categoryDao = categoryDao;
        this.directKvStore = directKvStore;
        this.categoriesCache = new HashMap<>();
        this.selectedCategories = new ArrayList<>();
    }

    //region Misc. utility methods
    public Comparator<CategoryItem> sortBySimilarity(final String filter) {
        Comparator<String> stringSimilarityComparator = StringSortingUtils.sortBySimilarity(filter);
        return (firstItem, secondItem) -> stringSimilarityComparator
                .compare(firstItem.getName(), secondItem.getName());
    }

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

    public void updateCategoryCount(CategoryItem item) {
        Category category = categoryDao.find(item.getName());

        // Newly used category...
        if (category == null) {
            category = new Category(null, item.getName(), new Date(), 0);
        }

        category.incTimesUsed();
        categoryDao.save(category);
    }
    //endregion

    //region Category Caching
    public void cacheAll(HashMap<String, ArrayList<String>> categories) {
        categoriesCache.putAll(categories);
    }

    public HashMap<String, ArrayList<String>> getCategoriesCache() {
        return categoriesCache;
    }

    boolean cacheContainsKey(String term) {
        return categoriesCache.containsKey(term);
    }
    //endregion

    //region Category searching
    public Observable<CategoryItem> searchAll(String term, List<String> imageTitleList) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return gpsCategories()
                    .concatWith(titleCategories(imageTitleList))
                    .concatWith(recentCategories());
        }

        //if user types in something that is in cache, return cached category
        if (cacheContainsKey(term)) {
            return Observable.fromIterable(getCachedCategories(term))
                    .map(name -> new CategoryItem(name, false));
        }

        //otherwise, search API for matching categories
        return mwApi
                .allCategories(term, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    public Observable<CategoryItem> searchCategories(String term, List<String> imageTitleList) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return gpsCategories()
                    .concatWith(titleCategories(imageTitleList))
                    .concatWith(recentCategories());
        }

        return mwApi
                .searchCategories(term, SEARCH_CATS_LIMIT)
                .map(s -> new CategoryItem(s, false));
    }

    private ArrayList<String> getCachedCategories(String term) {
        return categoriesCache.get(term);
    }

    public Observable<CategoryItem> defaultCategories(List<String> titleList) {
        Observable<CategoryItem> directCat = directCategories();
        if (hasDirectCategories()) {
            Timber.d("Image has direct Cat");
            return directCat
                    .concatWith(gpsCategories())
                    .concatWith(titleCategories(titleList))
                    .concatWith(recentCategories());
        } else {
            Timber.d("Image has no direct Cat");
            return gpsCategories()
                    .concatWith(titleCategories(titleList))
                    .concatWith(recentCategories());
        }
    }

    private boolean hasDirectCategories() {
        return !directKvStore.getString("Category", "").equals("");
    }

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

    Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(gpsCategoryModel.getCategoryList())
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> titleCategories(List<String> titleList) {
        return Observable.fromIterable(titleList)
                .concatMap(this::getTitleCategories);
    }

    private Observable<CategoryItem> getTitleCategories(String title) {
        return mwApi.searchTitles(title, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(categoryDao.recentCategories(SEARCH_CATS_LIMIT))
                .map(s -> new CategoryItem(s, false));
    }
    //endregion

    //region Category Selection
    @Override
    public void categoryClicked(CategoryItem item) {
        if (item.isSelected()) {
            selectCategory(item);
            updateCategoryCount(item);
        } else {
            unselectCategory(item);
        }
    }

    public void selectCategory(CategoryItem item) {
        selectedCategories.add(item);
    }

    public void unselectCategory(CategoryItem item) {
        selectedCategories.remove(item);
    }

    public int selectedCategoriesCount() {
        return selectedCategories.size();
    }

    public List<CategoryItem> getSelectedCategories() {
        return selectedCategories;
    }

    public List<String> getCategoryStringList() {
        List<String> output = new ArrayList<>();
        for (CategoryItem item : selectedCategories) {
            output.add(item.getName());
        }
        return output;
    }
    //endregion

}
