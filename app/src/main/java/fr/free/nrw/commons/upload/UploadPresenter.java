package fr.free.nrw.commons.upload;

import android.annotation.SuppressLint;
import fr.free.nrw.commons.CommonsApplication;
import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.filepicker.UploadableFile;
import fr.free.nrw.commons.kvstore.JsonKvStore;
import fr.free.nrw.commons.repository.UploadRepository;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements UploadContract.UserActionListener {

    private static final UploadContract.View DUMMY = (UploadContract.View) Proxy.newProxyInstance(
            UploadContract.View.class.getClassLoader(),
            new Class[]{UploadContract.View.class}, (proxy, method, methodArgs) -> null);
    private final UploadRepository repository;
    private final JsonKvStore defaultKvStore;
    private UploadContract.View view = DUMMY;

    private CompositeDisposable compositeDisposable;

    @Inject
    UploadPresenter(UploadRepository uploadRepository,
        @Named("default_preferences") JsonKvStore defaultKvStore) {
        this.repository = uploadRepository;
        this.defaultKvStore = defaultKvStore;
        compositeDisposable = new CompositeDisposable();
    }


    /**
     * Called by the submit button in {@link UploadActivity}
     */
    @SuppressLint("CheckResult")
    @Override
    public void handleSubmit() {
        if (view.isLoggedIn()) {
            view.showProgress(true);
            repository.buildContributions()
                    .observeOn(Schedulers.io())
                    .subscribe(new Observer<Contribution>() {
                        @Override
                        public void onSubscribe(Disposable d) {
                            view.showProgress(false);
                            if (defaultKvStore
                                .getBoolean(CommonsApplication.IS_LIMITED_CONNECTION_MODE_ENABLED,
                                    false)) {
                                view.showMessage(R.string.uploading_queued);
                            } else {
                                view.showMessage(R.string.uploading_started);
                            }

                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onNext(Contribution contribution) {
                            repository.prepareMedia(contribution);
                            contribution.setState(Contribution.STATE_QUEUED);
                            repository.saveContribution(contribution);
                        }

                        @Override
                        public void onError(Throwable e) {
                            view.showMessage(R.string.upload_failed);
                            repository.cleanup();
                            view.finish();
                            compositeDisposable.clear();
                            Timber.e("failed to upload: " + e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                            view.makeUploadRequest();
                            repository.cleanup();
                            view.finish();
                            compositeDisposable.clear();
                        }
                    });
        } else {
            view.askUserToLogIn();
        }
    }

    @Override
    public void deletePictureAtIndex(int index) {
        List<UploadableFile> uploadableFiles = view.getUploadableFiles();
        if (index == uploadableFiles.size() - 1) {//If the next fragment to be shown is not one of the MediaDetailsFragment, lets hide the top card
            view.showHideTopCard(false);
        }
        repository.deletePicture(uploadableFiles.get(index).getFilePath());
        if (uploadableFiles.size() == 1) {
            view.showMessage(R.string.upload_cancelled);
            view.finish();
            return;
        } else {
            view.onUploadMediaDeleted(index);
        }
        if (uploadableFiles.size() < 2) {
            view.showHideTopCard(false);
        }

        //In case lets update the number of uploadable media
        view.updateTopCardTitle();

    }

    @Override
    public void onAttachView(UploadContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
        compositeDisposable.clear();
        repository.cleanup();
    }

}
