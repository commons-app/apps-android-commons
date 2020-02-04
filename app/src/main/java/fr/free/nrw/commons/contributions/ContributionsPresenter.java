package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.Media;
import fr.free.nrw.commons.auth.SessionManager;
import fr.free.nrw.commons.contributions.ContributionsContract.UserActionListener;
import fr.free.nrw.commons.db.AppDatabase;
import fr.free.nrw.commons.mwapi.UserClient;
import fr.free.nrw.commons.utils.NetworkUtils;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.Scheduler;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import static fr.free.nrw.commons.contributions.Contribution.STATE_COMPLETED;

/**
 * The presenter class for Contributions
 */
public class ContributionsPresenter implements UserActionListener {

    private final ContributionsRepository repository;
    private CompositeDisposable compositeDisposable;
    private ContributionsContract.View view;
    private List<Contribution> contributionList;

    @Inject
    Context context;

    @Inject
    UserClient userClient;

    @Inject
    AppDatabase appDatabase;

    @Inject
    SessionManager sessionManager;

    @Inject
    ContributionsPresenter(ContributionsRepository repository) {
        this.repository = repository;
        compositeDisposable=new CompositeDisposable();
    }

    private String user;

    @Override
    public void onAttachView(ContributionsContract.View view) {
        this.view = view;
        compositeDisposable=new CompositeDisposable();
    }

    void fetchContributions() {
        repository.fetchContributions(repository.get("uploads"))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Observer<List<Contribution>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onNext(List<Contribution> contributions) {
;                        showContributions(contributions);
                    }

                    @Override
                    public void onError(Throwable e) {
                        view.showProgress(false);
                        //TODO
                    }

                    @Override
                    public void onComplete() {

                    }
                });
        if (NetworkUtils.isInternetConnectionEstablished(CommonsApplication.getInstance()) && shouldFetchContributions()) {
            view.showProgress(true);
            this.user = sessionManager.getUserName();
            view.showContributions(null);
            contributionList=new ArrayList<>();
            compositeDisposable.add(userClient.logEvents(user)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(mwQueryLogEvent -> Timber.d("Received image %s", mwQueryLogEvent.title()))
                    .filter(mwQueryLogEvent -> !mwQueryLogEvent.isDeleted()).doOnNext(mwQueryLogEvent -> Timber.d("Image %s passed filters", mwQueryLogEvent.title()))
                    .map(image -> {
                        Contribution contribution = new Contribution(null, null, image.title(),
                                "", -1, image.date(), image.date(), user,
                                "", "", STATE_COMPLETED);
                        return contribution;
                    })
                    .toList()
                    .subscribe(contributions -> {
                        showContributions(contributions);
                        saveContributionsToDB(contributions);
                    }, error -> {
                        //DO nothing,
                    }));
        }
    }

    private void showContributions(List<Contribution> contributions) {
        view.showProgress(false);
        if(!(contributions==null && contributions.size()==0)){
            view.showWelcomeTip(false);
            view.showNoContributionsUI(false);
            view.setUploadCount(contributions.size());
            view.showContributions(contributions);
            this.contributionList.clear();
            this.contributionList.addAll(contributions);
        } else {
            view.showWelcomeTip(true);
            view.showNoContributionsUI(true);
        }
    }

    private void saveContributionsToDB(List<Contribution> contributions) {
        appDatabase.getContributionDao().save(contributions)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<Long>>() {
                    @Override
                    public void onSubscribe(Disposable d) {

                    }

                    @Override
                    public void onSuccess(List<Long> longs) {
                        Log.d("CP","inserted contributios");
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("CP","failed to insert contributios");
                    }
                });
    }

    private boolean shouldFetchContributions() {
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
     * @param contribution
     */
    @Override
    public void deleteUpload(Contribution contribution) {
        repository.deleteContributionFromDB(contribution);
    }

    /**
     * Returns a contribution at the specified cursor position
     * @param i
     * @return
     */
    @Nullable
    @Override
    public Media getItemAtPosition(int i) {
        return  contributionList==null || contributionList.size()<i?null:contributionList.get(i);
    }
}
