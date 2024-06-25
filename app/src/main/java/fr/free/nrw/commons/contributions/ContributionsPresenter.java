package fr.free.nrw.commons.contributions;

import androidx.work.ExistingWorkPolicy;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import fr.free.nrw.commons.di.CommonsApplicationModule;
import fr.free.nrw.commons.upload.worker.WorkRequestHelper;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
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

}
