package fr.free.nrw.commons.upload;


import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.paging.DataSource;
import androidx.paging.DataSource.Factory;
import androidx.paging.LivePagedListBuilder;
import androidx.paging.PagedList;
import androidx.work.ExistingWorkPolicy;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.contributions.ContributionBoundaryCallback;
import fr.free.nrw.commons.contributions.ContributionsRemoteDataSource;
import fr.free.nrw.commons.contributions.ContributionsRepository;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.nearby.contract.NearbyParentFragmentContract;
import fr.free.nrw.commons.upload.PendingUploadsContract.UserActionListener;
import fr.free.nrw.commons.upload.PendingUploadsContract.View;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * The presenter class for Contributions
 */
public class PendingUploadsPresenter implements UserActionListener {

    private final ContributionBoundaryCallback contributionBoundaryCallback;
    private final ContributionsRepository repository;
    private final Scheduler ioThreadScheduler;

    private final CompositeDisposable compositeDisposable;
    private final ContributionsRemoteDataSource contributionsRemoteDataSource;

    LiveData<PagedList<Contribution>> totalContributionList;

    @Inject
    PendingUploadsPresenter(
        final ContributionBoundaryCallback contributionBoundaryCallback,
        final ContributionsRemoteDataSource contributionsRemoteDataSource,
        final ContributionsRepository repository,
        @Named(CommonsApplicationModule.IO_THREAD) final Scheduler ioThreadScheduler) {
        this.contributionBoundaryCallback = contributionBoundaryCallback;
        this.repository = repository;
        this.ioThreadScheduler = ioThreadScheduler;
        this.contributionsRemoteDataSource=contributionsRemoteDataSource;
        compositeDisposable = new CompositeDisposable();
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
            factory = repository.fetchContributions();
        }

        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory, pagedListConfig);
        if (shouldSetBoundaryCallback) {
            livePagedListBuilder.setBoundaryCallback(contributionBoundaryCallback);
        }

        totalContributionList = livePagedListBuilder.build();
    }

    @Override
    public void onAttachView(@NonNull View view) {

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
    public void deleteUpload(final Contribution contribution, Context context) {
        compositeDisposable.add(repository
            .deleteContributionFromDB(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void pauseUploads(List<Contribution> l, int index, Context context) {
        if (index >= l.size()) {
            return;
        }
        Contribution it = l.get(index);
        CommonsApplication.pauseUploads.put(it.getPageId().toString(), true);
        //Retain the paused state in DB
        it.setState(Contribution.STATE_PAUSED);
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                    pauseUploads(l, index + 1, context);
                }
            )
            .subscribe( () ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void deleteUploads(List<Contribution> l, int index, Context context) {
        if (index >= l.size()) {
            return;
        }
        Contribution it = l.get(index);
        compositeDisposable.add(repository
            .deleteContributionFromDB(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                    CommonsApplication.cancelledUploads.add(it.getPageId());
                    deleteUploads(l, index + 1, context);
                }
            )
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void restartUploads(List<Contribution> l, int index, Context context) {
        if (index >= l.size()) {
            return;
        }
        Contribution it = l.get(index);
        it.setState(Contribution.STATE_QUEUED);
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                CommonsApplication.pauseUploads.put(it.getPageId().toString(), false);
                    restartUploads(l, index + 1, context);
                }
            )
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void restartUpload(List<Contribution> l, int index, Context context) {
        if (index >= l.size()) {
            return;
        }
        Contribution it = l.get(index);
        it.setState(Contribution.STATE_QUEUED);
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    /**
     * Update the contribution's state in the databse, upon completion, trigger the workmanager to
     * process this contribution
     *
     * @param contribution
     */
    public void saveContribution(Contribution contribution, Context context) {
        compositeDisposable.add(repository
            .save(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                context, ExistingWorkPolicy.KEEP)));
    }

}
