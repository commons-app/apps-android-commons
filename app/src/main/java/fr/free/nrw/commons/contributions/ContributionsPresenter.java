package fr.free.nrw.commons.contributions;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.MediaDataExtractor;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import fr.free.nrw.commons.di.CommonsApplicationModule;
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

import javax.inject.Inject;
import javax.inject.Named;

/**
 * The presenter class for Contributions
 */
public class ContributionsPresenter implements UserActionListener {

    private final ContributionsRepository repository;
    private final Scheduler mainThreadScheduler;
    private final Scheduler ioThreadScheduler;
    private CompositeDisposable compositeDisposable;
    private ContributionsContract.View view;

    @Inject
    MediaDataExtractor mediaDataExtractor;

    @Inject
    ContributionsPresenter(ContributionsRepository repository, @Named(CommonsApplicationModule.MAIN_THREAD) Scheduler mainThreadScheduler,@Named(CommonsApplicationModule.IO_THREAD) Scheduler ioThreadScheduler) {
        this.repository = repository;
        this.mainThreadScheduler=mainThreadScheduler;
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
            .getMediaFromFileName(contribution.getFilename())
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(media -> {
                contribution.setThumbUrl(media.getThumbUrl());
                updateContribution(contribution);
            }));
    }
}
