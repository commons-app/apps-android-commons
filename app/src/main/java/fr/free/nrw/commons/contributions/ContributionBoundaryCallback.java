package fr.free.nrw.commons.contributions;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.paging.PagedList;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.media.MediaClient;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

public class ContributionBoundaryCallback extends PagedList.BoundaryCallback<Contribution> {

  public static final String TAG = "ItemKeyedUserDataSource";
  private final ContributionsRepository repository;
  private final SessionManager sessionManager;
  private final MediaClient mediaClient;
  private final MutableLiveData networkState;
  private final MutableLiveData initialLoading;
  private final CompositeDisposable compositeDisposable;
  private final Scheduler ioThreadScheduler;

  @Inject
  public ContributionBoundaryCallback(final ContributionsRepository repository,
      final SessionManager sessionManager,
      final MediaClient mediaClient,
      @Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
    super();
    this.ioThreadScheduler = ioThreadScheduler;
    networkState = new MutableLiveData();
    initialLoading = new MutableLiveData();
    this.repository = repository;
    this.sessionManager = sessionManager;
    this.mediaClient = mediaClient;
    compositeDisposable = new CompositeDisposable();
  }

  public MutableLiveData getNetworkState() {
    return networkState;
  }

  public MutableLiveData getInitialLoading() {
    return initialLoading;
  }

  @Override
  public void onZeroItemsLoaded() {
    Timber.d("On zero item loaded");
    fetchContributions();
  }

  @Override
  public void onItemAtFrontLoaded(@NonNull final Contribution itemAtFront) {
    Timber.d("On item front");
  }

  @Override
  public void onItemAtEndLoaded(@NonNull final Contribution itemAtEnd) {
    fetchContributions();
  }

  public void fetchContributions() {
    networkState.postValue(NetworkState.LOADING);
    compositeDisposable.add(mediaClient.getMediaListForUser(sessionManager.getUserName())
        .map(mediaList -> {
          List<Contribution> contributions = new ArrayList<>();
          for (Media media : mediaList) {
            contributions.add(new Contribution(media, Contribution.STATE_COMPLETED));
          }
          return contributions;
        })
        .subscribeOn(ioThreadScheduler)
        .subscribe(this::saveContributionsToDB, error -> {
          Timber.e("Failed to fetch contributions: %s", error.getMessage());
          networkState.postValue(NetworkState.FAILED);
        }));
  }

  private void saveContributionsToDB(final List<Contribution> contributions) {
    Single<List<Long>> single = repository.save(contributions);
    Timber.d(Arrays.toString(single.blockingGet().toArray()));
    repository.set("last_fetch_timestamp", System.currentTimeMillis());
    networkState.postValue(NetworkState.LOADED);
  }

}

