package fr.free.nrw.commons.contributions;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
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
  private final ContributionsRepository repository;
  private final Scheduler ioThreadScheduler;

  private CompositeDisposable compositeDisposable;

  public LiveData<PagedList<Contribution>> contributionList;
  public MutableLiveData networkState;

  @Inject
  ContributionsListPresenter(
      final ContributionBoundaryCallback contributionBoundaryCallback,
      ContributionsRepository repository,
      @Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
    this.contributionBoundaryCallback = contributionBoundaryCallback;
    this.repository = repository;
    this.ioThreadScheduler = ioThreadScheduler;
    compositeDisposable = new CompositeDisposable();
  }

  @Override
  public void onAttachView(ContributionsListContract.View view) {
  }

  /**
   * Setup the paged list.
   * This method sets the configuration for paged list and ties it up with the live data object.
   * This method can be tweaked to update the lazy loading behavior of the contributions list
   */
  void setup() {
    final PagedList.Config pagedListConfig =
        (new PagedList.Config.Builder())
            .setEnablePlaceholders(true)
            .setPrefetchDistance(50)
            .setPageSize(10).build();
    networkState = contributionBoundaryCallback.getNetworkState();
    contributionList = (new LivePagedListBuilder(repository.fetchContributions(), pagedListConfig)
        .setBoundaryCallback(contributionBoundaryCallback)).build();
  }

  @Override
  public void onDetachView() {
    compositeDisposable.clear();
  }

  /**
   * Delete a failed contribution from the local db
   *
   * @param contribution
   */
  @Override
  public void deleteUpload(final Contribution contribution) {
    compositeDisposable.add(repository.deleteContributionFromDB(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

}
