package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModel;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;

public class LeaderboardListViewModel extends ViewModel {

    private DataSourceFactory dataSourceFactory;
    private LiveData<PagedList<LeaderboardList>> listLiveData;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    private LiveData<String> progressLoadStatus = new MutableLiveData<>();

    public LeaderboardListViewModel(OkHttpJsonApiClient okHttpJsonApiClient, SessionManager sessionManager) {
        dataSourceFactory = new DataSourceFactory(okHttpJsonApiClient, compositeDisposable, sessionManager);
        initializePaging();
    }


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

    public LiveData<String> getProgressLoadStatus() {
        return progressLoadStatus;
    }

    public LiveData<PagedList<LeaderboardList>> getListLiveData() {
        return listLiveData;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        compositeDisposable.clear();
    }

}
