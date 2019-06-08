package fr.free.nrw.commons.upload.structure.depicts;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.utils.StringSortingUtils;
import io.reactivex.Observable;
import timber.log.Timber;

public class DepictModel {
    private static final int SEARCH_DEPICTS_LIMIT = 25;

    private List<DepictedItem> selectedDepictedItems;
    private final DepictDao depictDao;
    private final MediaWikiApi mediaWikiApi;
    private final JsonKvStore directKvStore;

    private HashMap<String, ArrayList<String>> depictsCache;

    @Inject
    GpsDepictsModel gpsDepictsModel;

    @Inject
    public DepictModel(List<DepictedItem> selectedDepictedItems, DepictDao depictDao, MediaWikiApi mediaWikiApi, JsonKvStore directKvStore, HashMap<String, ArrayList<String>> depictsCache) {
        this.selectedDepictedItems = selectedDepictedItems;
        this.depictDao = depictDao;
        this.mediaWikiApi = mediaWikiApi;
        this.directKvStore = directKvStore;
        this.depictsCache = depictsCache;
    }

    public Comparator<DepictedItem> sortBySimilarity(final String filter) {
        Comparator<String> stringSimilarityComparator = StringSortingUtils.sortBySimilarity(filter);
        return (firstItem, secondItem) -> stringSimilarityComparator
                .compare(firstItem.getDepictsLabel(), secondItem.getDepictsLabel());
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

    public Observable<DepictedItem> searchCategories(String term, List<UploadMediaDetail> imageTitleList) {
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


    public void onDepictItemClicked(DepictedItem depictedItem){
        if (depictedItem.isSelected()){
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
                .map(name -> new DepictedItem(name, "", null,false));
    }

    private Observable<DepictedItem> titleDepicts(List<UploadMediaDetail> titleList) {
        return Observable.fromIterable(titleList)
                .concatMap(this::getTitleDepicts);
    }

    private Observable<DepictedItem> getTitleDepicts(UploadMediaDetail uploadMediaDetail) {
        return mediaWikiApi.searchTitles(uploadMediaDetail.getCaptionText(), SEARCH_DEPICTS_LIMIT)
                .map(name -> new DepictedItem(name, "", null,false));
    }

    private Observable<DepictedItem> recentDepicts() {
        return Observable.fromIterable(depictDao.recentDepicts(SEARCH_DEPICTS_LIMIT))
                .map(s -> new DepictedItem(s, "", null,false));
    }
}
