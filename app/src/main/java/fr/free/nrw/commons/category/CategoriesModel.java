package fr.free.nrw.commons.category;

import android.content.SharedPreferences;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.MwVolleyApi;
import fr.free.nrw.commons.utils.StringSortingUtils;
import io.reactivex.Observable;
import timber.log.Timber;

public class CategoriesModel {
    private static final int SEARCH_CATS_LIMIT = 25;

    private final MediaWikiApi mwApi;
    private final CategoryDao categoryDao;
    private final SharedPreferences prefs;
    private final SharedPreferences directPrefs;

    private HashMap<String, ArrayList<String>> categoriesCache;
    private List<CategoryItem> selectedCategories;

    @Inject
    public CategoriesModel(MediaWikiApi mwApi, CategoryDao categoryDao,
                           @Named("default_preferences") SharedPreferences prefs,
                           @Named("direct_nearby_upload_prefs") SharedPreferences directPrefs) {
        this.mwApi = mwApi;
        this.categoryDao = categoryDao;
        this.prefs = prefs;
        this.directPrefs = directPrefs;
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

    private boolean cacheContainsKey(String term) {
        return categoriesCache.containsKey(term);
    }
    //endregion

    //region Category searching
    public Observable<CategoryItem> searchAll(String term) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return Observable.empty();
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

    public Observable<CategoryItem> searchCategories(String term) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return Observable.empty();
        }

        return mwApi
                .searchCategories(term, SEARCH_CATS_LIMIT)
                .map(s -> new CategoryItem(s, false));
    }

    private ArrayList<String> getCachedCategories(String term) {
        return categoriesCache.get(term);
    }

    public Observable<CategoryItem> defaultCategories() {
        Observable<CategoryItem> directCat = directCategories();
        if (hasDirectCategories()) {
            Timber.d("Image has direct Cat");
            return directCat
                    .concatWith(gpsCategories())
                    .concatWith(titleCategories())
                    .concatWith(recentCategories());
        } else {
            Timber.d("Image has no direct Cat");
            return gpsCategories()
                    .concatWith(titleCategories())
                    .concatWith(recentCategories());
        }
    }

    private boolean hasDirectCategories() {
        return !directPrefs.getString("Category", "").equals("");
    }

    private Observable<CategoryItem> directCategories() {
        String directCategory = directPrefs.getString("Category", "");
        List<String> categoryList = new ArrayList<>();
        Timber.d("Direct category found: " + directCategory);

        if (!directCategory.equals("")) {
            categoryList.add(directCategory);
            Timber.d("DirectCat does not equal emptyString. Direct Cat list has " + categoryList);
        }
        return Observable.fromIterable(categoryList).map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> gpsCategories() {
        return Observable.fromIterable(
                MwVolleyApi.GpsCatExists.getGpsCatExists()
                        ? MwVolleyApi.getGpsCat() : new ArrayList<>())
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> titleCategories() {
        //Retrieve the title that was saved when user tapped submit icon
        String title = prefs.getString("Title", "");

        return mwApi
                .searchTitles(title, SEARCH_CATS_LIMIT)
                .map(name -> new CategoryItem(name, false));
    }

    private Observable<CategoryItem> recentCategories() {
        return Observable.fromIterable(categoryDao.recentCategories(SEARCH_CATS_LIMIT))
                .map(s -> new CategoryItem(s, false));
    }
    //endregion

    //region Category Selection
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
