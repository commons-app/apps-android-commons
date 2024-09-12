package fr.free.nrw.commons.upload;


import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
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
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.PendingUploadsContract.UserActionListener;
import fr.free.nrw.commons.upload.PendingUploadsContract.View;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * The presenter class for PendingUploadsFragment and FailedUploadsFragment
 */
public class PendingUploadsPresenter implements UserActionListener {

    private final ContributionBoundaryCallback contributionBoundaryCallback;
    private final ContributionsRepository contributionsRepository;
    private final UploadRepository uploadRepository;
    private final Scheduler ioThreadScheduler;

    private final CompositeDisposable compositeDisposable;
    private final ContributionsRemoteDataSource contributionsRemoteDataSource;

    LiveData<PagedList<Contribution>> totalContributionList;
    LiveData<PagedList<Contribution>> failedContributionList;

    @Inject
    PendingUploadsPresenter(
        final ContributionBoundaryCallback contributionBoundaryCallback,
        final ContributionsRemoteDataSource contributionsRemoteDataSource,
        final ContributionsRepository contributionsRepository,
        final UploadRepository uploadRepository,
        @Named(CommonsApplicationModule.IO_THREAD) final Scheduler ioThreadScheduler) {
        this.contributionBoundaryCallback = contributionBoundaryCallback;
        this.contributionsRepository = contributionsRepository;
        this.uploadRepository = uploadRepository;
        this.ioThreadScheduler = ioThreadScheduler;
        this.contributionsRemoteDataSource = contributionsRemoteDataSource;
        compositeDisposable = new CompositeDisposable();
    }

    /**
     * Setups the paged list of Pending Uploads. This method sets the configuration for paged list
     * and ties it up with the live data object. This method can be tweaked to update the lazy
     * loading behavior of the contributions list
     */
    void setup() {
        final PagedList.Config pagedListConfig =
            (new PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build();
        Factory<Integer, Contribution> factory;

        factory = contributionsRepository.fetchContributionsWithStatesSortedByDateUploadStarted(
            Arrays.asList(Contribution.STATE_QUEUED, Contribution.STATE_IN_PROGRESS,
                Contribution.STATE_PAUSED));
        LivePagedListBuilder livePagedListBuilder = new LivePagedListBuilder(factory,
            pagedListConfig);
        totalContributionList = livePagedListBuilder.build();
    }

    /**
     * Setups the paged list of Failed Uploads. This method sets the configuration for paged list
     * and ties it up with the live data object. This method can be tweaked to update the lazy
     * loading behavior of the contributions list
     */
    void getFailedContributions() {
        final PagedList.Config pagedListConfig =
            (new PagedList.Config.Builder())
                .setPrefetchDistance(50)
                .setPageSize(10).build();
        Factory<Integer, Contribution> factory;
        factory = contributionsRepository.fetchContributionsWithStatesSortedByDateUploadStarted(
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
     * Deletes the specified upload (contribution) from the database.
     *
     * @param contribution The contribution object representing the upload to be deleted.
     * @param context      The context in which the operation is being performed.
     */
    @Override
    public void deleteUpload(final Contribution contribution, Context context) {
        compositeDisposable.add(contributionsRepository
            .deleteContributionFromDB(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    /**
     * Pauses all the uploads by changing the state of contributions from STATE_QUEUED and
     * STATE_IN_PROGRESS to STATE_PAUSED in the database.
     */
    public void pauseUploads() {
        CommonsApplication.isPaused = true;
        compositeDisposable.add(contributionsRepository
            .updateContributionsWithStates(
                List.of(Contribution.STATE_QUEUED, Contribution.STATE_IN_PROGRESS),
                Contribution.STATE_PAUSED)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    /**
     * Deletes contributions from the database that match the specified states.
     *
     * @param states A list of integers representing the states of the contributions to be deleted.
     */
    public void deleteUploads(List<Integer> states) {
        compositeDisposable.add(contributionsRepository
            .deleteContributionsFromDBWithStates(states)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    /**
     * Restarts the uploads for the specified list of contributions starting from the given index.
     *
     * @param contributionList The list of contributions to be restarted.
     * @param index            The starting index in the list from which to restart uploads.
     * @param context          The context in which the operation is being performed.
     */
    public void restartUploads(List<Contribution> contributionList, int index, Context context) {
        CommonsApplication.isPaused = false;
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        if (it.getState() == Contribution.STATE_FAILED) {
            it.setDateUploadStarted(Calendar.getInstance().getTime());
            if (it.getErrorInfo() == null) {
                it.setChunkInfo(null);
                it.setTransferred(0);
            }
            compositeDisposable.add(uploadRepository
                .checkDuplicateImage(it.getLocalUriPath().getPath())
                .subscribeOn(ioThreadScheduler)
                .subscribe(imageCheckResult -> {
                    if (imageCheckResult == IMAGE_OK) {
                        it.setState(Contribution.STATE_QUEUED);
                        compositeDisposable.add(contributionsRepository
                            .save(it)
                            .subscribeOn(ioThreadScheduler)
                            .doOnComplete(() -> {
                                restartUploads(contributionList, index + 1, context);
                            })
                            .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                                context, ExistingWorkPolicy.KEEP)));
                    } else {
                        Timber.e("Contribution already exists");
                        compositeDisposable.add(contributionsRepository
                            .deleteContributionFromDB(it)
                            .subscribeOn(ioThreadScheduler).doOnComplete(() -> {
                                restartUploads(contributionList, index + 1, context);
                            })
                            .subscribe());
                    }
                }, throwable -> {
                    Timber.e(throwable);
                    restartUploads(contributionList, index + 1, context);
                }));
        } else {
            it.setState(Contribution.STATE_QUEUED);
            compositeDisposable.add(contributionsRepository
                .save(it)
                .subscribeOn(ioThreadScheduler)
                .doOnComplete(() -> {
                    restartUploads(contributionList, index + 1, context);
                })
                .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)));
        }
    }

    /**
     * Restarts the upload for the specified list of contributions for the given index.
     *
     * @param contributionList The list of contributions.
     * @param index            The index in the list which to be restarted.
     * @param context          The context in which the operation is being performed.
     */
    public void restartUpload(List<Contribution> contributionList, int index, Context context) {
        CommonsApplication.isPaused = false;
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        if (it.getState() == Contribution.STATE_FAILED) {
            it.setDateUploadStarted(Calendar.getInstance().getTime());
            if (it.getErrorInfo() == null) {
                it.setChunkInfo(null);
                it.setTransferred(0);
            }
            compositeDisposable.add(uploadRepository
                .checkDuplicateImage(it.getLocalUriPath().getPath())
                .subscribeOn(ioThreadScheduler)
                .subscribe(imageCheckResult -> {
                    if (imageCheckResult == IMAGE_OK) {
                        it.setState(Contribution.STATE_QUEUED);
                        compositeDisposable.add(contributionsRepository
                            .save(it)
                            .subscribeOn(ioThreadScheduler)
                            .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                                context, ExistingWorkPolicy.KEEP)));
                    } else {
                        Timber.e("Contribution already exists");
                        compositeDisposable.add(contributionsRepository
                            .deleteContributionFromDB(it)
                            .subscribeOn(ioThreadScheduler)
                            .subscribe());
                    }
                }));
        } else {
            it.setState(Contribution.STATE_QUEUED);
            compositeDisposable.add(contributionsRepository
                .save(it)
                .subscribeOn(ioThreadScheduler)
                .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                    context, ExistingWorkPolicy.KEEP)));
        }
    }

}
