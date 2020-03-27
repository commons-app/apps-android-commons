package fr.free.nrw.commons.explore.depictions;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

import fr.free.nrw.commons.di.CommonsDaggerSupportFragment;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * The presenter class for SearchDepictionsFragment
 */
public class SearchDepictionsFragmentPresenter extends CommonsDaggerSupportFragment implements SearchDepictionsFragmentContract.UserActionListener {

    /**
     * This creates a dynamic proxy instance of the class,
     * proxy is to control access to the target object
     * here our target object is the view.
     * Thus we when onDettach method of fragment is called we replace the binding of view to our object with the proxy instance
     */
    private static final SearchDepictionsFragmentContract.View DUMMY = (SearchDepictionsFragmentContract.View) Proxy
            .newProxyInstance(
                    SearchDepictionsFragmentContract.View.class.getClassLoader(),
                    new Class[]{SearchDepictionsFragmentContract.View.class},
                    (proxy, method, methodArgs) -> null);
    private static int TIMEOUT_SECONDS = 15;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    private final Scheduler ioScheduler;
    private final Scheduler mainThreadScheduler;

    boolean isLoadingDepictions;
    String query;
    RecentSearchesDao recentSearchesDao;
    DepictsClient depictsClient;
    JsonKvStore basicKvStore;
    private SearchDepictionsFragmentContract.View view = DUMMY;
    private List<DepictedItem> queryList = new ArrayList<>();
    int offset=0;
    int size = 0;

    @Inject
    public SearchDepictionsFragmentPresenter(@Named("default_preferences") JsonKvStore basicKvStore,
                                             RecentSearchesDao recentSearchesDao,
                                             DepictsClient depictsClient,
                                             @Named(IO_THREAD) Scheduler ioScheduler,
                                             @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.basicKvStore = basicKvStore;
        this.recentSearchesDao = recentSearchesDao;
        this.depictsClient = depictsClient;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
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
     * @param reInitialise
     */
    @Override
    public void updateDepictionList(String query, int pageSize, boolean reInitialise) {
        this.query = query;
        view.loadingDepictions();
        if (reInitialise) {
            size = 0;
        }
        saveQuery();
        compositeDisposable.add(depictsClient.searchForDepictions(query, 25, offset)
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
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
        view.initErrorView();
        view.showSnackbar();
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

    /**
     * Whenever a new query is initiated from the search activity clear the previous adapter
     * and add new value of the query
     */
    @Override
    public void initializeQuery(String query) {
        this.query = query;
        this.queryList.clear();
        offset = 0;//Reset the offset on query change
        compositeDisposable.clear();
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
            for (DepictedItem m : mediaList) {
                fetchThumbnailForEntityId(m.getId(), size++);
            }
        }
    }

    /**
     * After all the depicted items are loaded fetch thumbnail image for all the depicted items (if available)
     */
    @Override
    public void fetchThumbnailForEntityId(String entityId,int position) {
         compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(response -> {
                    view.onImageUrlFetched(response,position);
                }));
    }

}
