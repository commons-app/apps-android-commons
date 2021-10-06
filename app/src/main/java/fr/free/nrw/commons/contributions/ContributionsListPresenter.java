package fr.free.nrw.commons.contributions;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.DataSource.Factory;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import fr.free.nrw.commons.contributions.ContributionsListContract.UserActionListener;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The presenter class for Contributions
 */
public class ContributionsListPresenter implements UserActionListener {

  private final ContributionBoundaryCallback contributionBoundaryCallback;
  private final ContributionsRemoteDataSource contributionsRemoteDataSource;
  private final ContributionsRepository repository;
  private final Scheduler ioThreadScheduler;

  private final CompositeDisposable compositeDisposable;

  LiveData<PagedList<Contribution>> contributionList;

  @Inject
  ContributionsListPresenter(
      final ContributionBoundaryCallback contributionBoundaryCallback,
      final ContributionsRemoteDataSource contributionsRemoteDataSource,
      final ContributionsRepository repository,
      @Named(CommonsApplicationModule.IO_THREAD) final Scheduler ioThreadScheduler) {
    this.contributionBoundaryCallback = contributionBoundaryCallback;
    this.repository = repository;
    this.contributionsRemoteDataSource=contributionsRemoteDataSource;
    this.ioThreadScheduler = ioThreadScheduler;
    compositeDisposable = new CompositeDisposable();
  }

  @Override
  public void onAttachView(final ContributionsListContract.View view) {
  }

  /**
   * Setup the paged list. This method sets the configuration for paged list and ties it up with the
   * live data object. This method can be tweaked to update the lazy loading behavior of the
   * contributions list
   * @param userName
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
      factory = repository.fetchContributions();
    }

    LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory, pagedListConfig);
    if (shouldSetBoundaryCallback) {
      livePagedListBuilder.setBoundaryCallback(contributionBoundaryCallback);
    }

    contributionList = livePagedListBuilder.build();

  }

  @Override
  public void onDetachView() {
    compositeDisposable.clear();

    contributionsRemoteDataSource.dispose();
    contributionBoundaryCallback.dispose();
  }

  /**
   * Delete a failed contribution from the local db
   */
  @Override
  public void deleteUpload(final Contribution contribution) {
    compositeDisposable.add(repository
        .deleteContributionFromDB(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

}
