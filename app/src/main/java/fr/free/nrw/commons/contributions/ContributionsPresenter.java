package fr.free.nrw.commons.contributions;

import static fr.free.nrw.commons.utils.ImageUtils.IMAGE_OK;

import androidx.work.ExistingWorkPolicy;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import javax.inject.Inject;
import javax.inject.Named;
import timber.log.Timber;

/**
 * The presenter class for Contributions
 */
public class ContributionsPresenter implements UserActionListener {

    private final ContributionsRepository contributionsRepository;
    private final UploadRepository uploadRepository;
    private final Scheduler ioThreadScheduler;
    private CompositeDisposable compositeDisposable;
    private ContributionsContract.View view;

    @Inject
    MediaDataExtractor mediaDataExtractor;

    @Inject
    ContributionsPresenter(ContributionsRepository repository,
        UploadRepository uploadRepository,
        @Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
        this.contributionsRepository = repository;
        this.uploadRepository = uploadRepository;
        this.ioThreadScheduler = ioThreadScheduler;
    }

    @Override
    public void onAttachView(ContributionsContract.View view) {
        this.view = view;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onDetachView() {
        this.view = null;
        compositeDisposable.clear();
    }

    @Override
    public Contribution getContributionsWithTitle(String title) {
        return contributionsRepository.getContributionWithFileName(title);
    }

    /**
     * Checks if a contribution is a duplicate and restarts the contribution process if it is not.
     *
     * @param contribution The contribution to check and potentially restart.
     */
    public void checkDuplicateImageAndRestartContribution(Contribution contribution) {
        compositeDisposable.add(uploadRepository
            .checkDuplicateImage(contribution.getLocalUriPath().getPath())
            .subscribeOn(ioThreadScheduler)
            .subscribe(imageCheckResult -> {
                if (imageCheckResult == IMAGE_OK) {
                    contribution.setState(Contribution.STATE_QUEUED);
                    saveContribution(contribution);
                } else {
                    Timber.e("Contribution already exists");
                    compositeDisposable.add(contributionsRepository
                        .deleteContributionFromDB(contribution)
                        .subscribeOn(ioThreadScheduler)
                        .subscribe());
                }
            }));
    }

    /**
     * Update the contribution's state in the databse, upon completion, trigger the workmanager to
     * process this contribution
     *
     * @param contribution
     */
    public void saveContribution(Contribution contribution) {
        compositeDisposable.add(contributionsRepository
            .save(contribution)
            .subscribeOn(ioThreadScheduler)
            .subscribe(() -> WorkRequestHelper.Companion.makeOneTimeWorkRequest(
                view.getContext().getApplicationContext(), ExistingWorkPolicy.KEEP)));
    }
}
