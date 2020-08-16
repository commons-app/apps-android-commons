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
    private String duration;
    private String category;
    private int limit;
    private int offset;

    public DataSourceFactory(OkHttpJsonApiClient okHttpJsonApiClient, CompositeDisposable compositeDisposable,
        SessionManager sessionManager, String duration, String category, int limit, int offset) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.compositeDisposable = compositeDisposable;
        this.sessionManager = sessionManager;
        this.duration = duration;
        this.category = category;
        this.limit = limit;
        this.offset = offset;
        liveData = new MutableLiveData<>();
    }

    public MutableLiveData<DataSourceClass> getMutableLiveData() {
        return liveData;
    }

    @Override
    public DataSource<Integer, LeaderboardList> create() {
        DataSourceClass dataSourceClass = new DataSourceClass(okHttpJsonApiClient, sessionManager, duration, category, limit, offset);
        liveData.postValue(dataSourceClass);
        return dataSourceClass;
    }
}
