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

    public String getDuration() {
        return duration;
    }

    public void setDuration(final String duration) {
        this.duration = duration;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(final String category) {
        this.category = category;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(final int limit) {
        this.limit = limit;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(final int offset) {
        this.offset = offset;
    }

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
        DataSourceClass dataSourceClass = new DataSourceClass(okHttpJsonApiClient, sessionManager, duration, category, limit, offset);
        liveData.postValue(dataSourceClass);
        return dataSourceClass;
    }
}
