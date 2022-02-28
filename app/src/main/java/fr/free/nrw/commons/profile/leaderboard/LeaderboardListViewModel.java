package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.data.models.profile.LeaderboardList;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Extends the ViewModel class and creates the LeaderboardList View Model
 */
public class LeaderboardListViewModel extends ViewModel {

    private DataSourceFactory dataSourceFactory;
    private LiveData<PagedList<LeaderboardList>> listLiveData;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private LiveData<String> progressLoadStatus = new MutableLiveData<>();

    /**
     * Constructor for a new LeaderboardListViewModel
     * @param okHttpJsonApiClient
     * @param sessionManager
     */
    public LeaderboardListViewModel(OkHttpJsonApiClient okHttpJsonApiClient, SessionManager
        sessionManager) {

        dataSourceFactory = new DataSourceFactory(okHttpJsonApiClient,
            compositeDisposable, sessionManager);
        initializePaging();
    }


    /**
     * Initialises the paging
     */
    private void initializePaging() {

        PagedList.Config pagedListConfig =
            new PagedList.Config.Builder()
                .setEnablePlaceholders(false)
                .setInitialLoadSizeHint(PAGE_SIZE)
                .setPageSize(PAGE_SIZE).build();

        listLiveData = new LivePagedListBuilder<>(dataSourceFactory, pagedListConfig)
            .build();

        progressLoadStatus = Transformations
            .switchMap(dataSourceFactory.getMutableLiveData(), DataSourceClass::getProgressLiveStatus);

    }

    /**
     * Refreshes the paged list with the new params and starts the loading of new data
     * @param duration
     * @param category
     * @param limit
     * @param offset
     */
    public void refresh(String duration, String category, int limit, int offset) {
        dataSourceFactory.setDuration(duration);
        dataSourceFactory.setCategory(category);
        dataSourceFactory.setLimit(limit);
        dataSourceFactory.setOffset(offset);
        dataSourceFactory.getMutableLiveData().getValue().invalidate();
    }

    /**
     * Sets the new params for the paged list API calls
     * @param duration
     * @param category
     * @param limit
     * @param offset
     */
    public void setParams(String duration, String category, int limit, int offset) {
        dataSourceFactory.setDuration(duration);
        dataSourceFactory.setCategory(category);
        dataSourceFactory.setLimit(limit);
        dataSourceFactory.setOffset(offset);
    }

    /**
     * @return the loading status of paged list
     */
    public LiveData<String> getProgressLoadStatus() {
        return progressLoadStatus;
    }

    /**
     * @return the paged list with live data
     */
    public LiveData<PagedList<LeaderboardList>> getListLiveData() {
        return listLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

}
