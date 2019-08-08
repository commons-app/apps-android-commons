package fr.free.nrw.commons.explore.depictions;

import android.widget.ImageView;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.mwapi.MediaWikiApi;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SearchDepictionsFragmentPresenter extends CommonsDaggerSupportFragment implements SearchDepictionsFragmentContract.UserActionListener {

    private static final SearchDepictionsFragmentContract.View DUMMY = (SearchDepictionsFragmentContract.View) Proxy
            .newProxyInstance(
                    SearchDepictionsFragmentContract.View.class.getClassLoader(),
                    new Class[]{SearchDepictionsFragmentContract.View.class},
                    (proxy, method, methodArgs) -> null);
    private static int TIMEOUT_SECONDS = 15;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

    boolean isLoadingDepictions;
    String query;
    RecentSearchesDao recentSearchesDao;
    MediaWikiApi mwApi;
    DepictsClient depictsClient;
    MediaClient mediaClient;
    JsonKvStore basicKvStore;
    private SearchDepictionsFragmentContract.View view = DUMMY;
    private List<DepictedItem> queryList = new ArrayList<>();

    @Inject
    public SearchDepictionsFragmentPresenter(@Named("default_preferences") JsonKvStore basicKvStore, MediaWikiApi mwApi, RecentSearchesDao recentSearchesDao, DepictsClient depictsClient, MediaClient mediaClient) {
        this.basicKvStore = basicKvStore;
        this.mwApi = mwApi;
        this.recentSearchesDao = recentSearchesDao;
        this.depictsClient = depictsClient;
        this.mediaClient = mediaClient;
    }

    @Override
    public void onAttachView(SearchDepictionsFragmentContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    /**
     * Adds 25 more results to existing search results
     */
    @Override
    public void addDepictionsToList() {
        if (isLoadingDepictions) return;
        isLoadingDepictions = true;
        view.loadingDepictions();
        compositeDisposable.add(depictsClient.searchForDepictions(query, 25, queryList.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                .subscribe(this::handlePaginationSuccess, this::handleError));
    }

    /**
     * Called when user selects "Items" from Search Activity
     * to load the list of depictions from API
     *
     * @param query string searched in the Explore Activity
     */
    @Override
    public void updateDepictionList(String query) {
        this.query = query;
        queryList.clear();
        view.clearAdapter();
        view.loadingDepictions();
        saveQuery();
        compositeDisposable.add(depictsClient.searchForDepictions(query, 25, queryList.size())
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .doOnSubscribe(disposable -> saveQuery())
                .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                .subscribe(this::handleSuccess, this::handleError));
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     */

    private void handlePaginationSuccess(List<DepictedItem> mediaList) {
        queryList.addAll(mediaList);
        isLoadingDepictions = false;
        view.onSuccess(mediaList);
    }

    /**
     * Logs and handles API error scenario
     */

    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried depictions");
        try {
            view.initErrorView();
            view.showSnackbar();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * This method saves Search Query in the Recent Searches Database.
     */
    @Override
    public void saveQuery() {
        RecentSearch recentSearch = recentSearchesDao.find(query);

        // Newly searched query...
        if (recentSearch == null) {
            recentSearch = new RecentSearch(null, query, new Date());
        } else {
            recentSearch.setLastSearched(new Date());
        }
        recentSearchesDao.save(recentSearch);

    }

    @Override
    public void initializeQuery(String query) {
        this.query = query;

    }

    @Override
    public String getQuery() {
        return query;
    }

    /**
     * Handles the success scenario
     * it initializes the recycler view by adding items to the adapter
     */

    public void handleSuccess(List<DepictedItem> mediaList) {
        queryList = mediaList;
        if (mediaList == null || mediaList.isEmpty()) {
            view.initErrorView();
        } else {
            view.onSuccess(mediaList);
        }
    }

    @Override
    public void fetchThumbnailForEntityId(String entityId,int position) {
         compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(response -> {
                    Timber.e("line155" + response);
                    if (response != null)
                    view.onImageUrlFetched(response,position);
                }));
    }

}
