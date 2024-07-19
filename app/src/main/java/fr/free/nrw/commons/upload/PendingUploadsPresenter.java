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
import fr.free.nrw.commons.upload.PendingUploadsContract.UserActionListener;
import fr.free.nrw.commons.upload.PendingUploadsContract.View;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.util.Arrays;
import java.util.Collections;
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
    LiveData<PagedList<Contribution>> failedContributionList;

    @Inject
    PendingUploadsPresenter(
        final ContributionBoundaryCallback contributionBoundaryCallback,
        final ContributionsRemoteDataSource contributionsRemoteDataSource,
        final ContributionsRepository repository,
        @Named(CommonsApplicationModule.IO_THREAD) final Scheduler ioThreadScheduler) {
        this.contributionBoundaryCallback = contributionBoundaryCallback;
        this.repository = repository;
        this.ioThreadScheduler = ioThreadScheduler;
        this.contributionsRemoteDataSource = contributionsRemoteDataSource;
        compositeDisposable = new CompositeDisposable();
    }


    /**
     * Setup the paged list. This method sets the configuration for paged list and ties it up with
     * the live data object. This method can be tweaked to update the lazy loading behavior of the
     * contributions list
     */
    void setup() {
        final PagedList.Config pagedListConfig =
            (new PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build();
        Factory<Integer, Contribution> factory;

        factory = repository.fetchContributionsWithStates(
            Arrays.asList(Contribution.STATE_QUEUED, Contribution.STATE_IN_PROGRESS,
                Contribution.STATE_PAUSED));
        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
            pagedListConfig);
        totalContributionList = livePagedListBuilder.build();
    }

    void getFailedContributions() {
        final PagedList.Config pagedListConfig =
            (new PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build();
        Factory<Integer, Contribution> factory;
        factory = repository.fetchContributionsWithStates(
            Collections.singletonList(Contribution.STATE_FAILED));
        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
            pagedListConfig);
        failedContributionList = livePagedListBuilder.build();
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

    public void pauseUploads(List<Contribution> contributionList, int index, Context context) {
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        CommonsApplication.pauseUploads.put(it.getPageId().toString(), true);
        //Retain the paused state in DB
        it.setState(Contribution.STATE_PAUSED);
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                    pauseUploads(contributionList, index + 1, context);
                }
            )
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void deleteUploads(List<Contribution> contributionList, int index, Context context) {
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        compositeDisposable.add(repository
            .deleteContributionFromDB(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                    deleteUploads(contributionList, index + 1, context);
                }
            )
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)
            ));
    }

    public void restartUploads(List<Contribution> contributionList, int index, Context context) {
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        it.setState(Contribution.STATE_QUEUED);
        if (it.getErrorInfo() == null){
            it.setChunkInfo(null);
            it.setTransferred(0);
        }
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .doOnComplete(() -> {
                    CommonsApplication.pauseUploads.put(it.getPageId().toString(), false);
                    restartUploads(contributionList, index + 1, context);
                }
            )
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP),
                throwable -> {
                    Timber.e(throwable);
                    restartUploads(contributionList, index + 1, context);
                }
            ));
    }

    public void restartUpload(List<Contribution> contributionList, int index, Context context) {
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        if (it.getErrorInfo() == null){
            it.setChunkInfo(null);
            it.setTransferred(0);
        }
        it.setState(Contribution.STATE_QUEUED);
        compositeDisposable.add(repository
            .save(it)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() ->
                WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP),
                throwable -> {
                    Timber.e(throwable);
                }
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
