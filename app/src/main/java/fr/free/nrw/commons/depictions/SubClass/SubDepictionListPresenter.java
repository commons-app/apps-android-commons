package fr.free.nrw.commons.depictions.SubClass;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.explore.recentsearches.RecentSearch;
import fr.free.nrw.commons.explore.recentsearches.RecentSearchesDao;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class SubDepictionListPresenter implements SubDepictionListContract.UserActionListener {

    private static final SubDepictionListContract.View DUMMY = (SubDepictionListContract.View) Proxy
            .newProxyInstance(
                    SubDepictionListContract.View.class.getClassLoader(),
                    new Class[]{SubDepictionListContract.View.class},
                    (proxy, method, methodArgs) -> null);

    private  SubDepictionListContract.View view = DUMMY;
    RecentSearchesDao recentSearchesDao;
    String query;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();
    DepictsClient depictsClient;
    private static int TIMEOUT_SECONDS = 15;
    private List<DepictedItem> queryList = new ArrayList<>();

    @Inject
    public SubDepictionListPresenter(RecentSearchesDao recentSearchesDao, DepictsClient depictsClient) {
        this.recentSearchesDao = recentSearchesDao;
        this.depictsClient = depictsClient;
    }
    @Override
    public void onAttachView(SubDepictionListContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

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
    public void fetchThumbnailForEntityId(String entityId, int position) {
        compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(response -> {
                    Timber.e("line67" + response);
                    view.onImageUrlFetched(response,position);
                }));
    }

    @Override
    public void initSubDepictionList() {
        if (view.isParentDepiction()) {
            compositeDisposable.add(depictsClient.searchForDepictions(query, 25, 0)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .doOnSubscribe(disposable -> saveQuery())
                    .collect(ArrayList<DepictedItem>::new, ArrayList::add)
                    .subscribe(this::handleSuccess, this::handleError));
        } else {
            compositeDisposable.add(depictsClient.searchForDepictions(query, 25, 0)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                    .doOnSubscribe(disposable -> saveQuery())
                    .collect(ArrayList<DepictedItem>::new, ArrayList::add)
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
        }
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

}
