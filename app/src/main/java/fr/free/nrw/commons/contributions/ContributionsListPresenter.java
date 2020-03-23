package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.contributions.Contribution.STATE_COMPLETED;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsListContract.UserActionListener;
import fr.free.nrw.commons.contributions.ContributionsContract.View;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.media.MediaClient;
import fr.free.nrw.commons.mwapi.UserClient;
import fr.free.nrw.commons.utils.NetworkUtils;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.schedulers.Schedulers;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * The presenter class for Contributions
 */
public class ContributionsListPresenter implements UserActionListener {

  private final ContributionsRepository repository;
  private final Scheduler mainThreadScheduler;
  private final Scheduler ioThreadScheduler;

  @Inject
  SessionManager sessionManager;
  @Inject
  MediaDataExtractor mediaDataExtractor;
  @Inject
  MediaClient mediaClient;
  private CompositeDisposable compositeDisposable;
  private ContributionsListContract.View view;
  private List<Contribution> contributionList = new ArrayList<>();
  private LifecycleOwner lifeCycleOwner;
  private String user;

  @Inject
  ContributionsListPresenter(ContributionsRepository repository,
      @Named(CommonsApplicationModule.MAIN_THREAD) Scheduler mainThreadScheduler,
      @Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
    this.repository = repository;
    this.mainThreadScheduler = mainThreadScheduler;
    this.ioThreadScheduler = ioThreadScheduler;
  }

  @Override
  public void onAttachView(ContributionsListContract.View view) {
    this.view = view;
    compositeDisposable = new CompositeDisposable();
  }

  public void setLifeCycleOwner(LifecycleOwner lifeCycleOwner) {
    this.lifeCycleOwner = lifeCycleOwner;
  }

  public void fetchContributions() {
    Timber.d("RecyclerList fetchContribution.");
    if (NetworkUtils.isInternetConnectionEstablished(CommonsApplication.getInstance())
        && shouldFetchContributions()) {
      view.showProgress(true);
      this.user = sessionManager.getUserName();

      Timber.d("RecyclerList fetching contributions: %s", this.user);
      view.showContributions(Collections.emptyList());
      compositeDisposable.add(mediaClient.getMediaListForUser(user)
          .subscribeOn(ioThreadScheduler)
          .observeOn(mainThreadScheduler)
          .map(mediaList -> {
            Timber.d("RecyclerList Fetched %d contributions.", mediaList.size());
            List<Contribution> contributions = new ArrayList<>();
            for (Media media : mediaList) {
              contributions.add(new Contribution(media));
            }
            return contributions;
          })
          .subscribe(this::saveContributionsToDB, error -> {
            Timber.e("Failed to fetch contributions: %s", error.getMessage());
          }));
    }
  }

  public void setupLiveData() {
    LiveData<List<Contribution>> liveDataContributions = repository.fetchContributions();
    if (null != lifeCycleOwner) {
      liveDataContributions.observe(lifeCycleOwner, this::showContributions);
    }
  }

  private void showContributions(@NonNull List<Contribution> contributions) {
    view.showProgress(false);
    if (contributions.isEmpty()) {
      view.showWelcomeTip(true);
      view.showNoContributionsUI(true);
    } else {
      view.showWelcomeTip(false);
      view.showNoContributionsUI(false);
      view.setUploadCount(contributions.size());
      view.showContributions(contributions);
      this.contributionList.clear();
      this.contributionList.addAll(contributions);
    }
  }

  private void saveContributionsToDB(List<Contribution> contributions) {
    Timber.d("RecyclerList saved contributions to db %d.", contributions.size());
    Timber.e("Fetched: " + contributions.size() + " contributions " + " saving to db");
    repository.save(contributions).subscribeOn(ioThreadScheduler).subscribe();
    repository.set("last_fetch_timestamp", System.currentTimeMillis());
  }

  private boolean shouldFetchContributions() {
    long lastFetchTimestamp = repository.getLong("last_fetch_timestamp");
    Timber.d("last fetch timestamp: %s", lastFetchTimestamp);
    if (lastFetchTimestamp != 0) {
      return System.currentTimeMillis() - lastFetchTimestamp > 15 * 60 * 100;
    }
    Timber.d("should  fetch contributions: %s", true);
    return true;
  }

  @Override
  public void onDetachView() {
    this.view = null;
    compositeDisposable.clear();
  }

  @Override
  public Contribution getContributionsWithTitle(String title) {
    return repository.getContributionWithFileName(title);
  }

  /**
   * Delete a failed contribution from the local db
   *
   * @param contribution
   */
  @Override
  public void deleteUpload(Contribution contribution) {
    compositeDisposable.add(repository.deleteContributionFromDB(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

  /**
   * Returns a contribution at the specified cursor position
   *
   * @param i
   * @return
   */
  @Nullable
  @Override
  public Media getItemAtPosition(int i) {
    if (i == -1 || contributionList.size() < i + 1) {
      return null;
    }
    return contributionList.get(i);
  }

  @Override
  public void updateContribution(Contribution contribution) {
    compositeDisposable.add(repository
        .updateContribution(contribution)
        .subscribeOn(ioThreadScheduler)
        .subscribe());
  }

  @Override
  public void fetchMediaDetails(Contribution contribution) {
    compositeDisposable.add(mediaDataExtractor
        .getMediaFromFileName(contribution.filename)
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(media -> {
          contribution.thumbUrl = media.thumbUrl;
          updateContribution(contribution);
        }));
  }
}
