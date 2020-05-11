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

/**
 * Class that extends PagedList.BoundaryCallback for contributions list It defines the action that
 * is triggered for various boundary conditions in the list
 */
public class ContributionBoundaryCallback extends PagedList.BoundaryCallback<Contribution> {

  private final ContributionsRepository repository;
  private final SessionManager sessionManager;
  private final MediaClient mediaClient;
  private final CompositeDisposable compositeDisposable;
  private final Scheduler ioThreadScheduler;

  @Inject
  public ContributionBoundaryCallback(final ContributionsRepository repository,
      final SessionManager sessionManager,
      final MediaClient mediaClient,
      @Named(CommonsApplicationModule.IO_THREAD) final Scheduler ioThreadScheduler) {
    this.ioThreadScheduler = ioThreadScheduler;
    this.repository = repository;
    this.sessionManager = sessionManager;
    this.mediaClient = mediaClient;
    compositeDisposable = new CompositeDisposable();
  }

  /**
   * It is triggered when the list has no items User's Contributions are then fetched from the
   * network
   */
  @Override
  public void onZeroItemsLoaded() {
    fetchContributions();
  }

  /**
   * It is triggered when the user scrolls to the top of the list No action is taken at this point
   */
  @Override
  public void onItemAtFrontLoaded(@NonNull final Contribution itemAtFront) {
  }

  /**
   * It is triggered when the user scrolls to the end of the list User's Contributions are then
   * fetched from the network
   */
  @Override
  public void onItemAtEndLoaded(@NonNull final Contribution itemAtEnd) {
    fetchContributions();
  }

  /**
   * Fetches contributions using the MediaWiki API
   */
  public void fetchContributions() {
    compositeDisposable.add(mediaClient.getMediaListForUser(sessionManager.getUserName())
        .map(mediaList -> {
          List<Contribution> contributions = new ArrayList<>();
          for (final Media media : mediaList) {
            contributions.add(new Contribution(media, Contribution.STATE_COMPLETED));
          }
          return contributions;
        })
        .subscribeOn(ioThreadScheduler)
        .subscribe(this::saveContributionsToDB, error -> {
          Timber.e("Failed to fetch contributions: %s", error.getMessage());
        }));
  }

  /**
   * Saves the contributions the the local DB
   */
  private void saveContributionsToDB(final List<Contribution> contributions) {
    repository.save(contributions);
    repository.set("last_fetch_timestamp", System.currentTimeMillis());
  }
}

