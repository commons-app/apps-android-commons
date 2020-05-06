package fr.free.nrw.commons.depictions.subClass;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
* Presenter for parent classes and child classes of Depicted items in Explore
 */
public class SubDepictionListPresenter implements SubDepictionListContract.UserActionListener {

    /**
     * This creates a dynamic proxy instance of the class,
     * proxy is to control access to the target object
     * here our target object is the view.
     * Thus we when onDettach method of fragment is called we replace the binding of view to our object with the proxy instance
     */
    private static final SubDepictionListContract.View DUMMY = (SubDepictionListContract.View) Proxy
            .newProxyInstance(
                    SubDepictionListContract.View.class.getClassLoader(),
                    new Class[]{SubDepictionListContract.View.class},
                    (proxy, method, methodArgs) -> null);

    private final Scheduler ioScheduler;
    private final Scheduler mainThreadScheduler;
    private  SubDepictionListContract.View view = DUMMY;
    RecentSearchesDao recentSearchesDao;
    /**
     * Value of the search query
     */
    public String query;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    DepictsClient depictsClient;
    private List<DepictedItem> queryList = new ArrayList<>();
    OkHttpJsonApiClient okHttpJsonApiClient;
    /**
     * variable used to record the number of API calls already made for fetching Thumbnails
     */
    private int size = 0;

    @Inject
    public SubDepictionListPresenter(RecentSearchesDao recentSearchesDao, DepictsClient depictsClient, OkHttpJsonApiClient okHttpJsonApiClient,  @Named(IO_THREAD) Scheduler ioScheduler,
                                     @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.recentSearchesDao = recentSearchesDao;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        this.depictsClient = depictsClient;
        this.okHttpJsonApiClient = okHttpJsonApiClient;
    }
    @Override
    public void onAttachView(SubDepictionListContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    /**
     * Store the current query in Recent searches
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
     * Calls Wikibase APIs to fetch Thumbnail image for a given wikidata item
     */
    @Override
    public void fetchThumbnailForEntityId(String entityId, int position) {
        compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .subscribe(response -> {
                    view.onImageUrlFetched(response,position);
                }));
    }

    @Override
    public void initSubDepictionList(String qid, Boolean isParentClass) throws IOException {
        size = 0;
        if (isParentClass) {
            compositeDisposable.add(okHttpJsonApiClient.getParentQIDs(qid)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe(this::handleSuccess, this::handleError));
        } else {
            compositeDisposable.add(okHttpJsonApiClient.getChildQIDs(qid)
                    .subscribeOn(ioScheduler)
                    .observeOn(mainThreadScheduler)
                    .subscribe(this::handleSuccess, this::handleError));
        }

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
            for (DepictedItem m : mediaList) {
                fetchThumbnailForEntityId(m.getId(), size++);
            }
        }
    }

    /**
     * Logs and handles API error scenario
     */
    private void handleError(Throwable throwable) {
        Timber.e(throwable, "Error occurred while loading queried depictions");
        view.initErrorView();
        view.showSnackbar();
    }

}
