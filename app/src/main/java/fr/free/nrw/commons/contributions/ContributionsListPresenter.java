package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.DataSource;
import androidx.paging.DataSource.Factory;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import fr.free.nrw.commons.contributions.ContributionsListContract.UserActionListener;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import kotlin.Unit;
import kotlin.jvm.functions.Function0;

/**
 * The presenter class for Contributions
 */
public class ContributionsListPresenter implements UserActionListener {

    private final ContributionBoundaryCallback contributionBoundaryCallback;
    private final ContributionsRepository repository;
    private final Scheduler ioThreadScheduler;

    private final CompositeDisposable compositeDisposable;
    private final ContributionsRemoteDataSource contributionsRemoteDataSource;

    LiveData<PagedList<Contribution>> contributionList;

    private MutableLiveData<List<Contribution>> liveData = new MutableLiveData<>();

    private List<Contribution> existingContributions = new ArrayList<>();

    // Timer for polling new contributions
    private Handler pollingHandler;
    private Runnable pollingRunnable;
    private long pollingInterval = 1 * 60 * 1000L; // Poll every 1 minutes

    @Inject
    ContributionsListPresenter(
        final ContributionBoundaryCallback contributionBoundaryCallback,
        final ContributionsRemoteDataSource contributionsRemoteDataSource,
        final ContributionsRepository repository,
        @Named(IO_THREAD) final Scheduler ioThreadScheduler) {
        this.contributionBoundaryCallback = contributionBoundaryCallback;
        this.repository = repository;
        this.ioThreadScheduler = ioThreadScheduler;
        this.contributionsRemoteDataSource = contributionsRemoteDataSource;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onAttachView(final ContributionsListContract.View view) {
    }

    /**
     * Setup the paged list. This method sets the configuration for paged list and ties it up with
     * the live data object. This method can be tweaked to update the lazy loading behavior of the
     * contributions list
     */
    void setup(String userName, boolean isSelf) {
        final PagedList.Config pagedListConfig =
            (new PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build();
        Factory<Integer, Contribution> factory;
        boolean shouldSetBoundaryCallback;
        if (!isSelf) {
            //We don't want to persist contributions for other user's, therefore
            // creating a new DataSource for them
            contributionsRemoteDataSource.setUserName(userName);
            factory = new Factory<Integer, Contribution>() {
                @NonNull
                @Override
                public DataSource<Integer, Contribution> create() {
                    return contributionsRemoteDataSource;
                }
            };
            shouldSetBoundaryCallback = false;
        } else {
            contributionBoundaryCallback.setUserName(userName);
            shouldSetBoundaryCallback = true;
            factory = repository.fetchContributionsWithStates(
                Collections.singletonList(Contribution.STATE_COMPLETED));
        }

        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
            pagedListConfig);
        if (shouldSetBoundaryCallback) {
            livePagedListBuilder.setBoundaryCallback(contributionBoundaryCallback);
        }

        contributionList = livePagedListBuilder.build();
        contributionList.observeForever(pagedList -> {
            if (pagedList != null) {
                existingContributions.clear();
                existingContributions.addAll(pagedList);
                liveData.setValue(existingContributions); // Update liveData with the latest list
            }
        });
        // Start polling for new contributions
        startPollingForNewContributions();
    }

    @Override
    public void onDetachView() {
        compositeDisposable.clear();
        contributionsRemoteDataSource.dispose();
        contributionBoundaryCallback.dispose();
        stopPollingForNewContributions();
    }


    /**
     * It is used to refresh list.
     *
     * @param swipeRefreshLayout used to stop refresh animation when
     * refresh finishes.
     */
    @Override
    public void refreshList(final SwipeRefreshLayout swipeRefreshLayout) {
        contributionBoundaryCallback.refreshList(() -> {
            swipeRefreshLayout.setRefreshing(false);
            return Unit.INSTANCE;
        });
    }
    /**
     * Start polling for new contributions every 15 minutes.
     */
    private void startPollingForNewContributions() {
        if (pollingHandler != null) {
            stopPollingForNewContributions();
        }

        pollingHandler = new Handler(Looper.getMainLooper());
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                fetchNewContributions(); // Fetch new contributions in background
                pollingHandler.postDelayed(this, pollingInterval); // Repeat after the interval
            }
        };
        pollingHandler.post(pollingRunnable); // Start polling immediately
    }

    /**
     * Stop the polling task when the view is detached or the activity is paused.
     */
    private void stopPollingForNewContributions() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
            pollingHandler = null;
            pollingRunnable = null;
        }
    }

    public void appendContributions(List<Contribution> newContributions) {
        if (newContributions != null && !newContributions.isEmpty()) {
            existingContributions.addAll(newContributions);
            liveData.postValue(existingContributions);
        }
    }
    /**
     * Fetch new contributions from the server and append them to the existing list.
     */
    private void fetchNewContributions() {
        contributionsRemoteDataSource.fetchContributions(new ContributionsRemoteDataSource.LoadCallback<Contribution>() {
            @Override
            public void onResult(List<Contribution> newContributions) {
                if (newContributions != null && !newContributions.isEmpty()) {
                    appendContributions(newContributions); // Add new contributions
                }
            }
        });
    }




}
