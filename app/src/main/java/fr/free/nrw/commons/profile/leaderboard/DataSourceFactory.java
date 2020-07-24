package fr.free.nrw.commons.profile.leaderboard;

import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;

public class DataSourceFactory extends DataSource.Factory<Integer, LeaderboardList> {

    private MutableLiveData<DataSourceClass> liveData;
    private OkHttpJsonApiClient okHttpJsonApiClient;
    private CompositeDisposable compositeDisposable;
    private SessionManager sessionManager;

    public DataSourceFactory(OkHttpJsonApiClient okHttpJsonApiClient, CompositeDisposable compositeDisposable,
        SessionManager sessionManager) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.compositeDisposable = compositeDisposable;
        this.sessionManager = sessionManager;
        liveData = new MutableLiveData<>();
    }

    public MutableLiveData<DataSourceClass> getMutableLiveData() {
        return liveData;
    }

    @Override
    public DataSource<Integer, LeaderboardList> create() {
        DataSourceClass dataSourceClass = new DataSourceClass(okHttpJsonApiClient, sessionManager);
        liveData.postValue(dataSourceClass);
        return dataSourceClass;
    }
}
