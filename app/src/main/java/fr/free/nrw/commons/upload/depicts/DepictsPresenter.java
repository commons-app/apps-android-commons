package fr.free.nrw.commons.upload.depicts;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

import android.util.Log;

import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.UploadModel;
import fr.free.nrw.commons.upload.structure.depicts.DepictedItem;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

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
    private DepictsClient depictsClient;
    private static int TIMEOUT_SECONDS = 15;

    private CompositeDisposable compositeDisposable;

    @Inject
    public DepictsPresenter(UploadRepository uploadRepository, @Named(IO_THREAD) Scheduler ioScheduler,
                            @Named(MAIN_THREAD) Scheduler mainThreadScheduler, DepictsClient depictsClient) {
        this.repository = uploadRepository;
        this.ioScheduler = ioScheduler;
        this.mainThreadScheduler = mainThreadScheduler;
        this.depictsClient = depictsClient;
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
    public void searchForDepictions(String query) {
        List<DepictedItem> depictedItemList = new ArrayList<>();
        List<String> imageTitleList = getImageTitleList();
        Observable<DepictedItem> distinctDepictsObservable = Observable
                .fromIterable(repository.getSelectedDepictions())
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .doOnSubscribe(disposable -> {
                    view.showError(true);
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
                            view.showProgress(false);

                            if (null == depictedItemList || depictedItemList.isEmpty()) {
                                view.showError(true);
                            } else {
                                view.showError(false);
                                //Understand this is shitty, but yes, doing it the other way is even worse and adapter positions can not be trusted
                                for (int position = 0; position < depictedItemList.size();
                                        position++) {
                                    depictedItemList.get(position).setPosition(position);
                                }
                                view.setDepictsList(depictedItemList);
                            }
                        }
                );
        compositeDisposable.add(searchDepictsDisposable);
        view.setDepictsList(depictedItemList);
    }

    /**
     * Check if depictions were selected
     * from the depiction list
     */

    @Override
    public void verifyDepictions() {
        List<DepictedItem> selectedDepictions = repository.getSelectedDepictions();
        Log.e("depictsline118", selectedDepictions.size()+"");
        if (selectedDepictions != null && !selectedDepictions.isEmpty()) {
            repository.setSelectedDepictions(repository.getDepictionsEntityIdList());
            view.goToNextScreen();
        } else {
            view.noDepictionSelected();
        }
    }

    @Override
    public void fetchThumbnailForEntityId(String entityId, int position) {
        compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .timeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .subscribe(response -> {
                    Timber.e("line155" + response);
                    view.onImageUrlFetched(response,position);
                }));
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
