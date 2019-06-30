package fr.free.nrw.commons.upload.depicts;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import fr.free.nrw.commons.R;
import io.reactivex.Observable;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.UploadMediaDetail;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import io.reactivex.Scheduler;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import timber.log.Timber;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

@Singleton
public class DepictsPresenter implements DepictsContract.UserActionListener {

    private static final DepictsContract.View DUMMY = (DepictsContract.View) Proxy
            .newProxyInstance(
                    DepictsContract.View.class.getClassLoader(),
                    new Class[]{DepictsContract.View.class},
                    (proxy, method, methodArgs) -> null);


    private final Scheduler ioScheduler;
    private final Scheduler mainThreadScheduler;
    private DepictsContract.View view = DUMMY;
    private UploadRepository repository;

    private CompositeDisposable compositeDisposable;

    @Inject
    public DepictsPresenter(UploadRepository uploadRepository, @Named(IO_THREAD) Scheduler ioScheduler,
                            @Named(MAIN_THREAD) Scheduler mainThreadScheduler) {
        this.repository = uploadRepository;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        compositeDisposable = new CompositeDisposable();
    }

    @Override
    public void onAttachView(DepictsContract.View view) {
        this.view = view;
    }

    @Override
    public void onDetachView() {
        this.view = DUMMY;
    }

    @Override
    public void onNextButtonPressed() {
        view.goToNextScreen();
    }

    @Override
    public void onPreviousButtonClicked() {
        view.goToPreviousScreen();
    }

    @Override
    public void onDepictItemClicked(DepictedItem depictedItem) {
        repository.onDepictItemClicked(depictedItem);
    }

    /**
     * asks the repository to fetch depictions for the query
     *  @param query
     */

    @Override
    public void searchForDepictions(String query, List<UploadMediaDetail> mediaDetailList) {
        List<DepictedItem> depictedItemList = new ArrayList<>();
        List<String> imageTitleList = getImageTitleList();
        Observable<DepictedItem> distinctDepictsObservable = Observable
                .fromIterable(repository.getSelectedDepictions())
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .doOnSubscribe(disposable -> {
                    view.showError(null);
                    view.showProgress(true);
                    view.setDepictsList(null);
                })
                .observeOn(ioScheduler)
                .concatWith(
                        repository.searchAllEntities(query, imageTitleList)
                )
                .distinct();

        Disposable searchDepictsDisposable = distinctDepictsObservable
                .observeOn(mainThreadScheduler)
                .subscribe(
                        s -> depictedItemList.add(s),
                        Timber::e,
                        () -> {
                            view.setDepictsList(depictedItemList);
                            view.showProgress(false);

                            if (depictedItemList.isEmpty()) {
                                view.showError(String.valueOf(R.string.no_depiction_found));
                            }
                        }
                );
        compositeDisposable.add(searchDepictsDisposable);
        depictedItemList.add(new DepictedItem("monument", "desc", null, false));
        depictedItemList.add(new DepictedItem("scultpture", "desc", null, false));
        depictedItemList.add(new DepictedItem("monument", "desc", null, false));
        depictedItemList.add(new DepictedItem("scultpture", "desc", null, false));
        view.setDepictsList(depictedItemList);
    }

    /**
     * Returns image title list from UploadItem
     * @return
     */
    private List<String> getImageTitleList() {
        List<String> titleList = new ArrayList<>();
        for (UploadModel.UploadItem item : repository.getUploads()) {
            if (item.getTitle().isSet()) {
                titleList.add(item.getTitle().toString());
            }
        }
        return titleList;
    }
}
