package fr.free.nrw.commons.upload;

import static fr.free.nrw.commons.upload.UploadModel.UploadItem;

import android.annotation.SuppressLint;

import fr.free.nrw.commons.R;
import fr.free.nrw.commons.contributions.Contribution;
import fr.free.nrw.commons.repository.UploadRepository;
import io.reactivex.Observer;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * The MVP pattern presenter of Upload GUI
 */
@Singleton
public class UploadPresenter implements UploadContract.UserActionListener {

    private static final UploadContract.View DUMMY = (UploadContract.View) Proxy.newProxyInstance(
            UploadContract.View.class.getClassLoader(),
            new Class[]{UploadContract.View.class}, (proxy, method, methodArgs) -> null);
    private final UploadRepository repository;
    private UploadContract.View view = DUMMY;

    private static final SimilarImageInterface SIMILAR_IMAGE = (SimilarImageInterface) Proxy.newProxyInstance(SimilarImageInterface.class.getClassLoader(),
            new Class[]{SimilarImageInterface.class}, (proxy, method, methodArgs) -> null);

    private CompositeDisposable compositeDisposable;

    @Inject
    UploadPresenter(UploadRepository uploadRepository) {
        this.repository = uploadRepository;
        compositeDisposable=new CompositeDisposable();
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
                            view.showMessage(R.string.uploading_started);
                            compositeDisposable.add(d);
                        }

                        @Override
                        public void onNext(Contribution contribution) {
                            repository.startUpload(contribution);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Timber.e("failed to upload: "+e.getMessage());
                        }

                        @Override
                        public void onComplete() {
                            repository.cleanup();
                            view.finish();
                            compositeDisposable.clear();
                        }
                    });
        } else {
            view.askUserToLogIn();
        }
    }

    public List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadItem item : repository.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }

    @Override
    public void deletePicture(String filePath) {
        repository.deletePicture(filePath);
    }

    @Override
    public void onAttachView(UploadContract.View view) {
        this.view = view;
        repository.prepareService();
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
        compositeDisposable.clear();
        repository.cleanup();
    }

}