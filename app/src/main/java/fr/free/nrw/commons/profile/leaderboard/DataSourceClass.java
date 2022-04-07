package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADED;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADING;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PageKeyedDataSource;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.profile.models.LeaderboardList;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Objects;
import timber.log.Timber;

/**
 * This class will call the leaderboard API to get new list when the pagination is performed
 */
public class DataSourceClass extends PageKeyedDataSource<Integer, LeaderboardList> {

    private OkHttpJsonApiClient okHttpJsonApiClient;
    private SessionManager sessionManager;
    private MutableLiveData<String> progressLiveStatus;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();
    private String duration;
    private String category;
    private int limit;
    private int offset;

    /**
     * Initialise the Data Source Class with API params
     * @param okHttpJsonApiClient
     * @param sessionManager
     * @param duration
     * @param category
     * @param limit
     * @param offset
     */
    public DataSourceClass(OkHttpJsonApiClient okHttpJsonApiClient,SessionManager sessionManager,
        String duration, String category, int limit, int offset) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.sessionManager = sessionManager;
        this.duration = duration;
        this.category = category;
        this.limit = limit;
        this.offset = offset;
        progressLiveStatus = new MutableLiveData<>();
    }


    /**
     * @return the status of the list
     */
    public MutableLiveData<String> getProgressLiveStatus() {
        return progressLiveStatus;
    }

    /**
     * Loads the initial set of data from API
     * @param params
     * @param callback
     */
    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params,
        @NonNull LoadInitialCallback<Integer, LeaderboardList> callback) {

        compositeDisposable.add(okHttpJsonApiClient
                .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                    duration, category, String.valueOf(limit), String.valueOf(offset))
                .doOnSubscribe(disposable -> {
                    compositeDisposable.add(disposable);
                    progressLiveStatus.postValue(LOADING);
                }).subscribe(
                response -> {
                    if (response != null && response.getStatus() == 200) {
                        progressLiveStatus.postValue(LOADED);
                        callback.onResult(response.getLeaderboardList(), null, response.getLimit());
                    }
                },
                t -> {
                    Timber.e(t, "Fetching leaderboard statistics failed");
                    progressLiveStatus.postValue(LOADING);
                }
            ));

    }

    /**
     * Loads any data before the inital page is loaded
     * @param params
     * @param callback
     */
    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
        @NonNull LoadCallback<Integer, LeaderboardList> callback) {

    }

    /**
     * Loads the next set of data on scrolling with offset as the limit of the last set of data
     * @param params
     * @param callback
     */
    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
        @NonNull LoadCallback<Integer, LeaderboardList> callback) {
        compositeDisposable.add(okHttpJsonApiClient
            .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                duration, category, String.valueOf(limit), String.valueOf(params.key))
            .doOnSubscribe(disposable -> {
                compositeDisposable.add(disposable);
                progressLiveStatus.postValue(LOADING);
            }).subscribe(
                response -> {
                    if (response != null && response.getStatus() == 200) {
                        progressLiveStatus.postValue(LOADED);
                        callback.onResult(response.getLeaderboardList(), params.key + limit);
                    }
                },
                t -> {
                    Timber.e(t, "Fetching leaderboard statistics failed");
                    progressLiveStatus.postValue(LOADING);
                }
            ));
    }
}
