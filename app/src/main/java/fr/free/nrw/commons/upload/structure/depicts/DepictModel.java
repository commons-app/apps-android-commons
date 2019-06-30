package fr.free.nrw.commons.upload.structure.depicts;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.utils.StringSortingUtils;
import fr.free.nrw.commons.utils.ViewUtil;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class DepictModel {
    private static final int SEARCH_DEPICTS_LIMIT = 25;
    private final DepictDao depictDao;
    private final MediaWikiApi mediaWikiApi;
    private final DepictsInterface depictsInterface;
    private final JsonKvStore directKvStore;
    @Inject
    GpsDepictsModel gpsDepictsModel;
    private List<DepictedItem> selectedDepictedItems;
    private HashMap<String, ArrayList<String>> depictsCache;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public DepictModel(MediaWikiApi mediaWikiApi, DepictDao depictDao, @Named("default_preferences") JsonKvStore directKvStore, DepictsInterface depictsInterface) {
        this.mediaWikiApi = mediaWikiApi;
        this.depictDao = depictDao;
        this.directKvStore = directKvStore;
        this.depictsInterface = depictsInterface;
        this.depictsCache = new HashMap<>();
        this.selectedDepictedItems = new ArrayList<>();
    }

    public Comparator<DepictedItem> sortBySimilarity(final String filter) {
        Comparator<String> stringSimilarityComparator = StringSortingUtils.sortBySimilarity(filter);
        return (firstItem, secondItem) -> stringSimilarityComparator
                .compare(firstItem.getDepictsLabel(), secondItem.getDescription());
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

    public void cacheAll(HashMap<String, ArrayList<String>> categories) {
        depictsCache.putAll(categories);
    }

    public HashMap<String, ArrayList<String>> getDepictsCache() {
        return depictsCache;
    }

    boolean cacheContainsKey(String term) {
        return depictsCache.containsKey(term);
    }

    /*public Observable<DepictedItem> searchAll(String term, List<UploadMediaDetail> imageTitleList) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return gpsDepicts()
                    .concatWith(titleDepicts(imageTitleList))
                    .concatWith(recentDepicts());
        }

        //if user types in something that is in cache, return cached category
        if (cacheContainsKey(term)) {
            return Observable.fromIterable(getDepictsCache(term))
                    .map(name -> new DepictedItem(name, false));
        }

        //otherwise, search API for matching categories
        return mediaWikiApi
                .allCategories(term, SEARCH_DEPICTS_LIMIT)
                .map(name -> new DepictedItem(name, false));
    }*/

    /*public Observable<DepictedItem> searchCategories(String term, List<UploadMediaDetail> imageTitleList) {
        //If user hasn't typed anything in yet, get GPS and recent items
        if (TextUtils.isEmpty(term)) {
            return gpsDepicts()
                    .concatWith(titleDepicts(imageTitleList))
                    .concatWith(recentDepicts());
        }

        return mediaWikiApi
                .searchCategories(term, SEARCH_DEPICTS_LIMIT)
                .map(s -> new DepictedItem(s, "", null, false));
    }
*/

    public void onDepictItemClicked(DepictedItem depictedItem) {
        if (depictedItem.isSelected()) {
            selectDepictItem(depictedItem);
            updateDepictCount(depictedItem);
        } else {
            unselectCategory(depictedItem);
        }
    }

    private void unselectCategory(DepictedItem depictedItem) {
    }

    private void updateDepictCount(DepictedItem depictedItem) {
        Depiction depiction = depictDao.find(depictedItem.getDepictsLabel());

        if (depictedItem == null) {
            depiction = new Depiction(null, depictedItem.getDepictsLabel(), new Date(), 0);
        }

        depiction.incTimesUsed();
        depictDao.save(depiction);
    }

    private void selectDepictItem(DepictedItem depictedItem) {
        selectedDepictedItems.add(depictedItem);
    }

    Observable<DepictedItem> gpsDepicts() {
        return Observable.fromIterable(gpsDepictsModel.getCategoryList())
                .map(name -> new DepictedItem(name, "", null, false));
    }

    private Observable<DepictedItem> titleDepicts(List<String> titleList) {
        return Observable.fromIterable(titleList)
                .concatMap(this::getTitleDepicts);
    }

    private Observable<DepictedItem> getTitleDepicts(String title) {
        return mediaWikiApi.searchTitles(title, SEARCH_DEPICTS_LIMIT)
                .map(name -> new DepictedItem(name, "", null, false));
    }

    private Observable<DepictedItem> recentDepicts() {
        return Observable.fromIterable(depictDao.recentDepicts(SEARCH_DEPICTS_LIMIT))
                .map(s -> new DepictedItem(s, "", null, false));
    }

    /**
     * Get selected Depictions
     * @return selected depictions
     */

    public List<DepictedItem> getSelectedDepictions() {
        return selectedDepictedItems;
    }

    /**
     * Search for depictions
     * @param query
     * @param imageTitleList
     * @return
     */

    public Observable<DepictedItem> searchAllEntities(String query, List<String> imageTitleList) {
        if (TextUtils.isEmpty(query)) {
            Observable<DepictedItem> depictedItemObservable = gpsDepicts()
                    .concatWith(titleDepicts(imageTitleList));

            if (hasDirectDepictions()) {
                return null;
            }
            return depictedItemObservable;
        }

        return depictsInterface.searchForDepicts(query, String.valueOf(SEARCH_DEPICTS_LIMIT))
                .map(mwQueryResponse -> {
                      return new DepictedItem(mwQueryResponse.query(),"", null, false);
                        });

               //.map(name -> new DepictedItem(name,"",null, false));
    }

    private boolean hasDirectDepictions() {
        return false;
    }
}
