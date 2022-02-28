package fr.free.nrw.commons.profile.leaderboard;

import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.data.models.profile.LeaderboardList;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;

/**
 * This class will create a new instance of the data source class on pagination
 */
public class DataSourceFactory extends DataSource.Factory<Integer, LeaderboardList> {

    private MutableLiveData<DataSourceClass> liveData;
    private OkHttpJsonApiClient okHttpJsonApiClient;
    private CompositeDisposable compositeDisposable;
    private SessionManager sessionManager;
    private String duration;
    private String category;
    private int limit;
    private int offset;

    /**
     * Gets the current set leaderboard list duration
     */
    public String getDuration() {
        return duration;
    }

    /**
     * Sets the current set leaderboard duration with the new duration
     */
    public void setDuration(final String duration) {
        this.duration = duration;
    }

    /**
     * Gets the current set leaderboard list category
     */
    public String getCategory() {
        return category;
    }

    /**
     * Sets the current set leaderboard category with the new category
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /**
     * Gets the current set leaderboard list limit
     */
    public int getLimit() {
        return limit;
    }

    /**
     * Sets the current set leaderboard limit with the new limit
     */
    public void setLimit(final int limit) {
        this.limit = limit;
    }

    /**
     * Gets the current set leaderboard list offset
     */
    public int getOffset() {
        return offset;
    }

    /**
     * Sets the current set leaderboard offset with the new offset
     */
    public void setOffset(final int offset) {
        this.offset = offset;
    }

    /**
     * Constructor for DataSourceFactory class
     * @param okHttpJsonApiClient client for OKhttp
     * @param compositeDisposable composite disposable
     * @param sessionManager sessionManager
     */
    public DataSourceFactory(OkHttpJsonApiClient okHttpJsonApiClient, CompositeDisposable compositeDisposable,
        SessionManager sessionManager) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.compositeDisposable = compositeDisposable;
        this.sessionManager = sessionManager;
        liveData = new MutableLiveData<>();
    }

    /**
     * @return the live data
     */
    public MutableLiveData<DataSourceClass> getMutableLiveData() {
        return liveData;
    }

    /**
     * Creates the new instance of data source class
     * @return
     */
    @Override
    public DataSource<Integer, LeaderboardList> create() {
        DataSourceClass dataSourceClass = new DataSourceClass(okHttpJsonApiClient, sessionManager, duration, category, limit, offset);
        liveData.postValue(dataSourceClass);
        return dataSourceClass;
    }
}
