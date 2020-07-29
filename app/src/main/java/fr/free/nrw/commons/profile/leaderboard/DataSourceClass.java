package fr.free.nrw.commons.profile.leaderboard;

import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADED;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.LOADING;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.PAGE_SIZE;
import static fr.free.nrw.commons.profile.leaderboard.LeaderboardConstants.START_OFFSET;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PageKeyedDataSource;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.mwapi.OkHttpJsonApiClient;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Objects;
import timber.log.Timber;

public class DataSourceClass extends PageKeyedDataSource<Integer, LeaderboardList> {

    private OkHttpJsonApiClient okHttpJsonApiClient;
    private SessionManager sessionManager;
    private MutableLiveData<String> progressLiveStatus;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public DataSourceClass(OkHttpJsonApiClient okHttpJsonApiClient,SessionManager sessionManager) {
        this.okHttpJsonApiClient = okHttpJsonApiClient;
        this.sessionManager = sessionManager;
        progressLiveStatus = new MutableLiveData<>();
    }


    public MutableLiveData<String> getProgressLiveStatus() {
        return progressLiveStatus;
    }

    @Override
    public void loadInitial(@NonNull LoadInitialParams<Integer> params,
        @NonNull LoadInitialCallback<Integer, LeaderboardList> callback) {

        compositeDisposable.add(okHttpJsonApiClient
                .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                    "all_time", "upload", String.valueOf(PAGE_SIZE), String.valueOf(START_OFFSET))
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

    @Override
    public void loadBefore(@NonNull LoadParams<Integer> params,
        @NonNull LoadCallback<Integer, LeaderboardList> callback) {

    }

    @Override
    public void loadAfter(@NonNull LoadParams<Integer> params,
        @NonNull LoadCallback<Integer, LeaderboardList> callback) {
        compositeDisposable.add(okHttpJsonApiClient
            .getLeaderboard(Objects.requireNonNull(sessionManager.getCurrentAccount()).name,
                "all_time", "upload", String.valueOf(PAGE_SIZE), String.valueOf(params.key))
            .doOnSubscribe(disposable -> {
                compositeDisposable.add(disposable);
                progressLiveStatus.postValue(LOADING);
            }).subscribe(
                response -> {
                    if (response != null && response.getStatus() == 200) {
                        progressLiveStatus.postValue(LOADED);
                        callback.onResult(response.getLeaderboardList(), params.key + PAGE_SIZE);
                    }
                },
                t -> {
                    Timber.e(t, "Fetching leaderboard statistics failed");
                    progressLiveStatus.postValue(LOADING);
                }
            ));
    }
}
