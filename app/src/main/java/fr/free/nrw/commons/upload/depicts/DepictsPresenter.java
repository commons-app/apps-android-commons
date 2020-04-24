package fr.free.nrw.commons.upload.depicts;

import static fr.free.nrw.commons.di.CommonsApplicationModule.IO_THREAD;
import static fr.free.nrw.commons.di.CommonsApplicationModule.MAIN_THREAD;

import fr.free.nrw.commons.explore.depictions.DepictsClient;
import fr.free.nrw.commons.repository.UploadRepository;
import fr.free.nrw.commons.upload.structure.depictions.DepictedItem;
import io.reactivex.Observable;
import io.reactivex.Scheduler;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import timber.log.Timber;

/**
 * presenter for DepictsFragment
 */
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
        Observable<DepictedItem> distinctDepictsObservable = Observable
                .fromIterable(repository.getSelectedDepictions())
                .subscribeOn(ioScheduler)
                .observeOn(mainThreadScheduler)
                .doOnSubscribe(disposable -> {
                    view.showProgress(true);
                    view.setDepictsList(null);
                })
                .observeOn(ioScheduler)
                .concatWith(
                        repository.searchAllEntities(query)
                )
                .distinct();

        Disposable searchDepictsDisposable = distinctDepictsObservable
                .observeOn(mainThreadScheduler)
                .subscribe(
                    e -> {
                        depictedItemList.add(e);
                    },
                        t -> {
                            view.showProgress(false);
                            view.showError(true);
                            Timber.e(t);
                        },
                        () -> {
                            view.showProgress(false);

                            if (depictedItemList.isEmpty()) {
                                view.showError(true);
                            } else {
                                view.showError(false);
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
        if (selectedDepictions != null && !selectedDepictions.isEmpty()) {
            view.goToNextScreen();
        } else {
            view.noDepictionSelected();
        }
    }

    /**
     * Fetch thumbnail for the Wikidata Item
     * @param entityId entityId of the item
     * @param position position of the item
     */
    @Override
    public void fetchThumbnailForEntityId(String entityId, int position) {
        compositeDisposable.add(depictsClient.getP18ForItem(entityId)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(response -> {
                    view.onImageUrlFetched(response,position);
                }));
    }
}
