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
import java.util.Arrays;
import java.util.Calendar;
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

        factory = contributionsRepository.fetchContributionsWithStatesSortedByDateUploadStarted(
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

    @Override
    public void deleteUpload(final Contribution contribution, Context context) {
        compositeDisposable.add(contributionsRepository
            .deleteContributionFromDB(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    public void pauseUploads(List<Integer> states, int newState) {
        CommonsApplication.isPaused = true ;
        compositeDisposable.add(contributionsRepository
            .updateContributionWithStates(states, newState)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    public void deleteUploads(List<Integer> states) {
        compositeDisposable.add(contributionsRepository
            .deleteContributionsFromDBWithStates(states)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

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
                            .subscribe());
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
                .subscribe());
        }
    }

    public void restartUpload(List<Contribution> contributionList, int index, Context context) {
        CommonsApplication.isPaused = false;
        if (index >= contributionList.size()) {
            return;
        }
        Contribution it = contributionList.get(index);
        if (it.getState() == Contribution.STATE_FAILED) {
            it.setDateUploadStarted(Calendar.getInstance().getTime());
            if (it.getErrorInfo() == null){
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
                            .subscribe());
                    }else {
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
                .subscribe());
        }
    }

}
