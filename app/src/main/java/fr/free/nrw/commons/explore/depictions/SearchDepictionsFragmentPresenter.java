package fr.free.nrw.commons.explore.depictions;

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
    int offset=0;

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
     * Called when user selects "Items" from Search Activity
     * to load the list of depictions from API
     *
     * @param query string searched in the Explore Activity
     */
    @Override
    public void updateDepictionList(String query,int pageSize) {
        this.query = query;
        view.loadingDepictions();
        saveQuery();
        compositeDisposable.add(depictsClient.searchForDepictions(query, 25, offset)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .doOnSubscribe(disposable -> saveQuery())
                .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                .subscribe(this::handleSuccess, this::handleError));
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
        this.queryList.clear();
        offset=0;//Reset the offset on query change
        view.setIsLastPage(false);
        view.clearAdapter();
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
        if (mediaList == null || mediaList.isEmpty()) {
            if(queryList.isEmpty()){
                view.initErrorView();
            }else{
                view.setIsLastPage(true);
            }
        } else {
            this.queryList.addAll(mediaList);
            view.onSuccess(mediaList);
            offset=queryList.size();
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
                    view.onImageUrlFetched(response,position);
                }));
    }

}
