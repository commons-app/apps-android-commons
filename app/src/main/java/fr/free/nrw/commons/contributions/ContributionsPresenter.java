package fr.free.nrw.commons.contributions;

import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;

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
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
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
        if (NetworkUtils.isInternetConnectionEstablished(CommonsApplication.getInstance())) {
            view.showProgress(true);
            this.user = sessionManager.getUserName();
            view.showContributions(null);
            contributionList=new ArrayList<>();
            compositeDisposable.add(userClient.logEvents(user)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .doOnNext(mwQueryLogEvent -> Timber.d("Received image %s", mwQueryLogEvent.title()))
                    .filter(mwQueryLogEvent -> !mwQueryLogEvent.isDeleted()).doOnNext(mwQueryLogEvent -> Timber.d("Image %s passed filters", mwQueryLogEvent.title()))
                    .map(image -> new Contribution(null, null, image.title(),
                            "", -1, image.date(), image.date(), user,
                            "", "", STATE_COMPLETED)).buffer(100)
                    .subscribe(imageValues -> {
                        this.contributionList.addAll(imageValues);
                        view.showProgress(false);
                        if (imageValues != null && imageValues.size() > 0) {
                            view.showWelcomeTip(false);
                            view.showNoContributionsUI(false);
                            view.setUploadCount(imageValues.size());
                            view.showContributions(contributionList);
                        } else {
                            view.showWelcomeTip(true);
                            view.showNoContributionsUI(true);
                        }
                       Observable.fromIterable(contributionList)
                                .subscribeOn(Schedulers.io())
                                .doOnEach(imageValue -> appDatabase.getContributionDao().save(imageValue.getValue()));
                    }, error -> {
                        view.showProgress(false);
                        view.showMessage(error.getLocalizedMessage());
                        //TODO show error
                    }));
        }
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
        return  contributionList==null?null:contributionList.get(i);
    }

    /**
     * Get contribution position  with id
     */
    public int getChildPositionWithId(String fileName) {
        for (Contribution contribution : contributionList) {
            if (contribution.getFilename().equals(fileName)) {
                return contributionList.indexOf(contribution);
            }
        }
        return -1;
    }
}
