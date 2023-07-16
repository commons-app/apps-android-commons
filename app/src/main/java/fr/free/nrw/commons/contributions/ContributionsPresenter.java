package fr.free.nrw.commons.contributions;

import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.OutOfQuotaPolicy;
import androidx.work.WorkManager;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.upload.worker.UploadWorker;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.Consumer;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * The presenter class for Contributions
 */
public class ContributionsPresenter implements UserActionListener {

    private final ContributionsRepository repository;
    private final Scheduler ioThreadScheduler;
    private CompositeDisposable compositeDisposable;
    private ContributionsContract.View view;

    @Inject
    MediaDataExtractor mediaDataExtractor;

    @Inject
    ContributionsPresenter(ContributionsRepository repository,
        @Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
        this.repository = repository;
        this.ioThreadScheduler=ioThreadScheduler;
    }

    @Override
    public void onAttachView(ContributionsContract.View view) {
        this.view = view;
        compositeDisposable=new CompositeDisposable();
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
     * @param contribution
     */
    @Override
    public void deleteUpload(Contribution contribution) {
        compositeDisposable.add(repository
            .deleteContributionFromDB(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe());
    }

    /**
     * Update the contribution's state in the databse, upon completion, trigger the workmanager to
     * process this contribution
     *
     * @param contribution
     */
    @Override
    public void saveContribution(Contribution contribution) {
        compositeDisposable.add(repository
            .save(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() -> {
                OneTimeWorkRequest updatedUploadRequest = new OneTimeWorkRequest
                    .Builder(UploadWorker.class)
                    .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                    .build();
                WorkManager.getInstance(view.getContext().getApplicationContext())
                    .enqueueUniqueWork(
                        UploadWorker.class.getSimpleName(),
                        ExistingWorkPolicy.KEEP, updatedUploadRequest);
            }));
    }
}
