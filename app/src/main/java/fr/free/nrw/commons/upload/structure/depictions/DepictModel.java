package fr.free.nrw.commons.upload.structure.depictions;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.depicts.DepictsInterface;
import fr.free.nrw.commons.utils.StringSortingUtils;
import fr.free.nrw.commons.wikidata.model.DepictSearchItem;
import io.reactivex.Observable;
import io.reactivex.disposables.CompositeDisposable;
import timber.log.Timber;


/**
 * The model class for depictions in upload
 */
public class DepictModel {
    private static final int SEARCH_DEPICTS_LIMIT = 25;
    private final DepictionDao depictDao;
    private final DepictsInterface depictsInterface;
    private final JsonKvStore directKvStore;
    @Inject
    DepictsClient depictsClient;

    private List<DepictedItem> selectedDepictedItems;
    private HashMap<String, ArrayList<String>> depictsCache;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Inject
    public DepictModel(DepictionDao depictDao, @Named("default_preferences") JsonKvStore directKvStore, DepictsInterface depictsInterface) {
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

    public void cacheAll(HashMap<String, ArrayList<String>> depictsCache) {
        depictsCache.putAll(depictsCache);
    }

    public HashMap<String, ArrayList<String>> getDepictsCache() {
        return depictsCache;
    }

    boolean cacheContainsKey(String term) {
        return depictsCache.containsKey(term);
    }

    public void onDepictItemClicked(DepictedItem depictedItem) {
        if (depictedItem.isSelected()) {
            selectDepictItem(depictedItem);
//            updateDepictCount(depictedItem);
        } else {
            unselectDepiction(depictedItem);
        }
    }

    private void unselectDepiction(DepictedItem depictedItem) {
        selectedDepictedItems.remove(depictedItem);
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

    private Observable<DepictedItem> titleDepicts(List<String> titleList) {
        return Observable.fromIterable(titleList)
                .concatMap(this::getTitleDepicts);
    }

    private Observable<DepictedItem> getTitleDepicts(String title) {
        return depictsInterface.searchForDepicts(title, String.valueOf(SEARCH_DEPICTS_LIMIT), Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage(),"0")
                .map(depictSearchResponse -> {
                    DepictSearchItem depictedItem = depictSearchResponse.getSearch().get(0);
                   return new DepictedItem(depictedItem.getLabel(), depictedItem.getDescription(), "", false, depictedItem.getId());
                });
    }

    private Observable<DepictedItem> recentDepicts() {
        return Observable.fromIterable(depictDao.recentDepicts(SEARCH_DEPICTS_LIMIT))
                .map(s -> new DepictedItem(s, "", "", false, ""));
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
        return depictsInterface.searchForDepicts(query, String.valueOf(SEARCH_DEPICTS_LIMIT), Locale.getDefault().getLanguage(), Locale.getDefault().getLanguage(), "0")
                .flatMap(depictSearchResponse -> Observable.fromIterable(depictSearchResponse.getSearch()))
                .map(depictSearchItem -> new DepictedItem(depictSearchItem.getLabel(), depictSearchItem.getDescription(), "", false, depictSearchItem.getId()));
    }

    public List<String> depictionsEntityIdList() {
        List<String> output = new ArrayList<>();
        for (DepictedItem d : selectedDepictedItems) {
            output.add(d.getEntityId());
        }
        return output;
    }
}
